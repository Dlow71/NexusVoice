#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
gRPC到RTP TTS桥接进程

功能：
1. 通过gRPC双向流接收TTS音频（PCM 24kHz/48kHz）
2. 重采样：24kHz/16kHz → 48kHz mono
3. 编码：PCM → Opus 48kHz@40kbps
4. RTP封包：20ms帧
5. UDP发送到KMS（127.0.0.1:50061）
6. 支持静音帧注入（打断场景）

作者：NexusVoice Team
时间：2025-11-01
"""

import argparse
import logging
import sys
import time
import threading
import queue
import gi

gi.require_version('Gst', '1.0')
gi.require_version('GstApp', '1.0')
from gi.repository import Gst, GstApp, GLib

import grpc
from generated import tts_service_pb2
from generated import tts_service_pb2_grpc
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


class Grpc2RtpTtsBridge:
    """gRPC到RTP TTS桥接器"""
    
    def __init__(self, args):
        self.args = args
        self.pipeline = None
        self.appsrc = None
        self.grpc_channel = None
        self.grpc_stub = None
        self.request_observer = None
        self.audio_queue = queue.Queue(maxsize=100)
        self.running = False
        self.interrupted = False
        
    def build_pipeline(self):
        """构建GStreamer管道"""
        logger.info("构建GStreamer管道...")
        
        # 管道描述：
        # appsrc（应用层推送PCM） → audioconvert
        # → audioresample（重采样到48k） → audio/x-raw,rate=48000,channels=1
        # → opusenc（编码Opus） → rtpopuspay（RTP封包）
        # → udpsink（UDP发送到KMS）
        
        pipeline_desc = f"""
        appsrc name=src is-live=true format=time do-timestamp=true
            caps="audio/x-raw,rate=24000,channels=1,format=S16LE"
        ! audioconvert
        ! audioresample
        ! audio/x-raw,rate=48000,channels=1,format=S16LE
        ! opusenc bitrate={self.args.opus_bitrate} frame-size=20
        ! rtpopuspay pt=96
        ! udpsink host={self.args.rtp_host} port={self.args.rtp_port}
        """
        
        self.pipeline = Gst.parse_launch(pipeline_desc)
        
        # 获取appsrc
        self.appsrc = self.pipeline.get_by_name('src')
        
        # 配置appsrc回调
        self.appsrc.connect('need-data', self.on_need_data)
        
        logger.info(f"GStreamer管道构建成功，目标 {self.args.rtp_host}:{self.args.rtp_port}")
        
    def connect_grpc(self):
        """连接gRPC TTS服务"""
        logger.info(f"连接TTS gRPC服务: {self.args.grpc_host}:{self.args.grpc_port}")
        
        # 创建gRPC通道
        self.grpc_channel = grpc.insecure_channel(
            f'{self.args.grpc_host}:{self.args.grpc_port}',
            options=[
                ('grpc.max_send_message_length', 32 * 1024 * 1024),
                ('grpc.max_receive_message_length', 32 * 1024 * 1024),
                ('grpc.keepalive_time_ms', 20000),
                ('grpc.keepalive_timeout_ms', 60000),
            ]
        )
        
        self.grpc_stub = tts_service_pb2_grpc.TtsServiceStub(self.grpc_channel)
        
        # 创建流式会话
        self.request_observer = self.grpc_stub.StreamSynthesize(self.handle_tts_responses)
        
        # 发送配置消息
        config = tts_service_pb2.TtsConfig(
            session_id=self.args.session_id,
            voice_id='zh_female_wwxkjx',
            language_code='zh-CN',
            audio_format=common_pb2.PCM_16BIT,
            sample_rate=24000,
            channels=1,
            speed=1.0,
            volume=0.8,
            pitch=1.0,
            emotion=tts_service_pb2.NEUTRAL
        )
        
        self.request_observer.on_next(tts_service_pb2.TtsRequest(config=config))
        logger.info("TTS配置已发送")
        
    def handle_tts_responses(self, response_iterator):
        """处理gRPC TTS响应"""
        try:
            for response in response_iterator:
                if response.HasField('audio'):
                    audio = response.audio
                    
                    # 检查是否为静音帧
                    if audio.is_silence:
                        logger.debug(f"收到静音帧: segmentId={audio.segment_id}")
                        # 注入静音（打断场景）
                        silence = b'\x00' * 960  # 20ms@24kHz单声道S16LE = 960字节
                        self.audio_queue.put(silence)
                    else:
                        # 正常音频数据
                        logger.debug(f"收到TTS音频: segmentId={audio.segment_id}, bytes={len(audio.audio_content)}")
                        self.audio_queue.put(bytes(audio.audio_content))
                        
                elif response.HasField('progress'):
                    progress = response.progress
                    logger.debug(f"TTS合成进度: {progress.progress_percentage}%")
                    
                elif response.HasField('error'):
                    error = response.error
                    logger.error(f"TTS错误: code={error.error_code}, message={error.error_message}")
                    
                elif response.HasField('status'):
                    status = response.status
                    logger.info(f"TTS会话状态: state={status.state}")
                    
        except Exception as e:
            logger.error(f"处理TTS响应失败: {e}")
            
    def on_need_data(self, src, length):
        """GStreamer回调：需要更多数据"""
        try:
            # 从队列获取音频数据（非阻塞）
            audio_data = self.audio_queue.get(timeout=0.1)
            
            # 创建GStreamer缓冲区
            buffer = Gst.Buffer.new_allocate(None, len(audio_data), None)
            buffer.fill(0, audio_data)
            
            # 推送到管道
            ret = self.appsrc.emit('push-buffer', buffer)
            
            if ret != Gst.FlowReturn.OK:
                logger.warning(f"推送音频缓冲区失败: {ret}")
                
        except queue.Empty:
            # 队列为空，推送静音
            silence = b'\x00' * 960  # 20ms静音
            buffer = Gst.Buffer.new_allocate(None, len(silence), None)
            buffer.fill(0, silence)
            self.appsrc.emit('push-buffer', buffer)
            
        except Exception as e:
            logger.error(f"处理need-data失败: {e}")
            
    def send_text_to_tts(self, text, segment_id=0, is_last=True):
        """发送文本到TTS（用于测试）"""
        if not self.request_observer:
            logger.warning("TTS gRPC流未建立")
            return
            
        try:
            text_data = tts_service_pb2.TextData(
                text=text,
                segment_id=segment_id,
                is_last=is_last,
                timestamp_ms=int(time.time() * 1000),
                is_ssml=False
            )
            
            request = tts_service_pb2.TtsRequest(text=text_data)
            self.request_observer.on_next(request)
            
            logger.info(f"发送文本到TTS: text='{text}', segmentId={segment_id}")
            
        except Exception as e:
            logger.error(f"发送文本到TTS失败: {e}")
            
    def run(self):
        """启动桥接进程"""
        logger.info("启动gRPC→RTP TTS桥接进程...")
        logger.info(f"配置: grpc={self.args.grpc_host}:{self.args.grpc_port}, "
                   f"rtp={self.args.rtp_host}:{self.args.rtp_port}, "
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
            
            # 4. 测试：发送示例文本（可选）
            if self.args.test_text:
                time.sleep(2)  # 等待管道就绪
                self.send_text_to_tts(self.args.test_text)
            
            # 5. 进入主循环
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
        
        # 发送结束信号
        if self.request_observer:
            try:
                end_signal = common_pb2.EndSignal(
                    reason=common_pb2.NORMAL_END,
                    message='桥接进程正常关闭'
                )
                self.request_observer.on_next(tts_service_pb2.TtsRequest(end=end_signal))
                self.request_observer.on_completed()
            except Exception as e:
                logger.error(f"发送TTS结束信号失败: {e}")
        
        # 停止GStreamer管道
        if self.pipeline:
            self.pipeline.set_state(Gst.State.NULL)
            logger.info("GStreamer管道已停止")
            
        # 关闭gRPC连接
        if self.grpc_channel:
            self.grpc_channel.close()
            logger.info("gRPC连接已关闭")


def main():
    parser = argparse.ArgumentParser(description='gRPC到RTP TTS桥接进程')
    parser.add_argument('--grpc-host', default='localhost', help='TTS gRPC服务地址')
    parser.add_argument('--grpc-port', type=int, default=50052, help='TTS gRPC服务端口')
    parser.add_argument('--rtp-host', default='127.0.0.1', help='RTP目标地址（KMS）')
    parser.add_argument('--rtp-port', type=int, default=50061, help='RTP目标端口（KMS）')
    parser.add_argument('--session-id', required=True, help='会话ID')
    parser.add_argument('--opus-bitrate', type=int, default=40000, help='Opus编码码率（bps）')
    parser.add_argument('--test-text', default='', help='测试文本（可选）')
    parser.add_argument('--debug', action='store_true', help='启用调试日志')
    
    args = parser.parse_args()
    
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)
        
    bridge = Grpc2RtpTtsBridge(args)
    bridge.run()


if __name__ == '__main__':
    main()






