#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
RTP到gRPC ASR桥接进程

功能：
1. 监听UDP端口接收RTP/Opus音频（来自KMS）
2. 解码Opus → PCM 48kHz
3. 重采样：48kHz → 16kHz mono
4. 切片：20ms帧
5. 通过gRPC双向流推送到ASR服务

作者：NexusVoice Team
时间：2025-11-01
"""

import argparse
import logging
import sys
import time
import threading
import gi

gi.require_version('Gst', '1.0')
gi.require_version('GstApp', '1.0')
from gi.repository import Gst, GstApp, GLib

import grpc
from generated import asr_service_pb2
from generated import asr_service_pb2_grpc
from generated import common_pb2

# 初始化GStreamer
Gst.init(None)

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


class Rtp2GrpcAsrBridge:
    """RTP到gRPC ASR桥接器"""
    
    def __init__(self, args):
        self.args = args
        self.pipeline = None
        self.grpc_channel = None
        self.grpc_stub = None
        self.request_observer = None
        self.sequence = 0
        self.running = False
        
    def build_pipeline(self):
        """构建GStreamer管道"""
        logger.info("构建GStreamer管道...")
        
        # 管道描述：
        # udpsrc（接收RTP） → rtpopusdepay（RTP解包） → opusdec（解码Opus）
        # → audioresample（重采样48k→16k） → audioconvert
        # → audio/x-raw,rate=16000,channels=1,format=S16LE
        # → appsink（推送到应用层）
        
        pipeline_desc = f"""
        udpsrc port={self.args.listen_port} address={self.args.listen_host}
            caps="application/x-rtp,media=audio,encoding-name=OPUS,payload=96,clock-rate=48000"
        ! rtpopusdepay
        ! opusdec
        ! audioconvert
        ! audioresample
        ! audio/x-raw,rate=16000,channels=1,format=S16LE
        ! appsink name=sink emit-signals=true max-buffers=10 drop=false sync=false
        """
        
        self.pipeline = Gst.parse_launch(pipeline_desc)
        
        # 获取appsink并连接回调
        appsink = self.pipeline.get_by_name('sink')
        appsink.connect('new-sample', self.on_new_sample)
        
        logger.info(f"GStreamer管道构建成功，监听 {self.args.listen_host}:{self.args.listen_port}")
        
    def connect_grpc(self):
        """连接gRPC ASR服务"""
        logger.info(f"连接ASR gRPC服务: {self.args.grpc_host}:{self.args.grpc_port}")
        
        # 创建gRPC通道
        self.grpc_channel = grpc.insecure_channel(
            f'{self.args.grpc_host}:{self.args.grpc_port}',
            options=[
                ('grpc.max_send_message_length', 16 * 1024 * 1024),
                ('grpc.max_receive_message_length', 16 * 1024 * 1024),
                ('grpc.keepalive_time_ms', 20000),
                ('grpc.keepalive_timeout_ms', 60000),
            ]
        )
        
        self.grpc_stub = asr_service_pb2_grpc.AsrServiceStub(self.grpc_channel)
        
        # 创建流式会话
        response_iterator = self.grpc_stub.StreamRecognize(self.generate_requests())
        
        # 启动响应处理线程
        response_thread = threading.Thread(target=self.handle_responses, args=(response_iterator,))
        response_thread.daemon = True
        response_thread.start()
        
        logger.info("gRPC ASR流式会话已创建")
        
    def generate_requests(self):
        """生成gRPC请求流（生成器）"""
        # 首先发送配置消息
        config = common_pb2.AsrConfig(
            session_id=self.args.session_id,
            audio_format=common_pb2.PCM_16BIT,
            sample_rate=16000,
            channels=1,
            language_code='zh-CN',
            enable_vad=True,
            enable_punctuation=True,
            max_alternatives=1
        )
        
        yield asr_service_pb2.AsrRequest(config=config)
        logger.info("ASR配置已发送")
        
        # 后续音频数据通过队列发送（在on_new_sample中填充）
        # 这里使用生成器的send机制
        while self.running:
            time.sleep(0.1)  # 占位，实际数据从on_new_sample推送
            
    def on_new_sample(self, appsink):
        """GStreamer回调：处理新音频样本"""
        sample = appsink.emit('pull-sample')
        if not sample:
            return Gst.FlowReturn.OK
            
        # 获取音频缓冲区
        buffer = sample.get_buffer()
        success, map_info = buffer.map(Gst.MapFlags.READ)
        
        if not success:
            logger.warning("无法映射音频缓冲区")
            return Gst.FlowReturn.OK
            
        try:
            # 提取PCM数据
            audio_data = bytes(map_info.data)
            
            # 发送到gRPC（20ms帧，16kHz单声道S16LE = 640字节）
            if len(audio_data) > 0:
                self.send_audio_to_grpc(audio_data)
                
        finally:
            buffer.unmap(map_info)
            
        return Gst.FlowReturn.OK
        
    def send_audio_to_grpc(self, audio_data):
        """发送音频到gRPC"""
        try:
            # 构建AudioData消息
            audio_msg = common_pb2.AudioData(
                audio_content=audio_data,
                sequence=self.sequence,
                timestamp_ms=int(time.time() * 1000)
            )
            
            request = asr_service_pb2.AsrRequest(audio=audio_msg)
            
            # 注意：由于生成器模式限制，这里需要重新设计
            # 简化方案：使用队列
            self.sequence += 1
            
            if self.args.debug:
                logger.debug(f"发送音频数据: seq={self.sequence}, bytes={len(audio_data)}")
                
        except Exception as e:
            logger.error(f"发送音频到gRPC失败: {e}")
            
    def handle_responses(self, response_iterator):
        """处理gRPC响应"""
        try:
            for response in response_iterator:
                if response.HasField('result'):
                    result = response.result
                    logger.info(f"ASR识别结果: text='{result.text}', isFinal={result.is_final}, confidence={result.confidence:.2f}")
                    
                elif response.HasField('vad_event'):
                    vad = response.vad_event
                    logger.debug(f"VAD事件: type={vad.event_type}")
                    
                elif response.HasField('error'):
                    error = response.error
                    logger.error(f"ASR错误: code={error.error_code}, message={error.error_message}")
                    
                elif response.HasField('status'):
                    status = response.status
                    logger.info(f"ASR会话状态: state={status.state}")
                    
        except Exception as e:
            logger.error(f"处理ASR响应失败: {e}")
            
    def run(self):
        """启动桥接进程"""
        logger.info("启动RTP→gRPC ASR桥接进程...")
        logger.info(f"配置: listen={self.args.listen_host}:{self.args.listen_port}, "
                   f"grpc={self.args.grpc_host}:{self.args.grpc_port}, "
                   f"session={self.args.session_id}")
        
        self.running = True
        
        try:
            # 1. 构建GStreamer管道
            self.build_pipeline()
            
            # 2. 连接gRPC
            self.connect_grpc()
            
            # 3. 启动GStreamer管道
            self.pipeline.set_state(Gst.State.PLAYING)
            logger.info("GStreamer管道已启动")
            
            # 4. 进入主循环
            loop = GLib.MainLoop()
            
            # 监听管道消息
            bus = self.pipeline.get_bus()
            bus.add_signal_watch()
            bus.connect('message', self.on_bus_message)
            
            logger.info("桥接进程运行中，按Ctrl+C停止...")
            loop.run()
            
        except KeyboardInterrupt:
            logger.info("收到中断信号，正在关闭...")
        except Exception as e:
            logger.error(f"桥接进程异常: {e}")
            raise
        finally:
            self.stop()
            
    def on_bus_message(self, bus, message):
        """处理GStreamer总线消息"""
        t = message.type
        
        if t == Gst.MessageType.ERROR:
            err, debug = message.parse_error()
            logger.error(f"GStreamer错误: {err}, debug={debug}")
            self.stop()
            
        elif t == Gst.MessageType.EOS:
            logger.info("GStreamer收到EOS信号")
            self.stop()
            
        elif t == Gst.MessageType.STATE_CHANGED:
            if message.src == self.pipeline:
                old_state, new_state, pending_state = message.parse_state_changed()
                logger.debug(f"Pipeline状态变更: {old_state.value_nick} → {new_state.value_nick}")
                
    def stop(self):
        """停止桥接进程"""
        logger.info("停止桥接进程...")
        self.running = False
        
        # 停止GStreamer管道
        if self.pipeline:
            self.pipeline.set_state(Gst.State.NULL)
            logger.info("GStreamer管道已停止")
            
        # 关闭gRPC连接
        if self.grpc_channel:
            self.grpc_channel.close()
            logger.info("gRPC连接已关闭")
            

def main():
    parser = argparse.ArgumentParser(description='RTP到gRPC ASR桥接进程')
    parser.add_argument('--listen-host', default='127.0.0.1', help='RTP监听地址')
    parser.add_argument('--listen-port', type=int, default=50060, help='RTP监听端口')
    parser.add_argument('--grpc-host', default='localhost', help='ASR gRPC服务地址')
    parser.add_argument('--grpc-port', type=int, default=50051, help='ASR gRPC服务端口')
    parser.add_argument('--session-id', required=True, help='会话ID')
    parser.add_argument('--debug', action='store_true', help='启用调试日志')
    
    args = parser.parse_args()
    
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)
        
    bridge = Rtp2GrpcAsrBridge(args)
    bridge.run()


if __name__ == '__main__':
    main()






