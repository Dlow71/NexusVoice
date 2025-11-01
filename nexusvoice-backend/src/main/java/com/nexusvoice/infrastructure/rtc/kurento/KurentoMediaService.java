package com.nexusvoice.infrastructure.rtc.kurento;

import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kurento媒体服务
 * 
 * 功能：管理Kurento媒体管道和端点
 * 职责：
 * - 创建和释放Pipeline
 * - 创建和管理WebRtcEndpoint
 * - 创建和管理RtpEndpoint
 * - 连接端点和媒体流
 * - 条件化加载（仅当rtc.enabled=true时生效）
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "rtc.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class KurentoMediaService {

    private final KurentoClient kurentoClient;
    
    // 会话Pipeline缓存（sessionId -> MediaPipeline）
    private final Map<String, MediaPipeline> pipelines = new ConcurrentHashMap<>();
    
    // 会话WebRtcEndpoint缓存（sessionId -> WebRtcEndpoint）
    private final Map<String, WebRtcEndpoint> webRtcEndpoints = new ConcurrentHashMap<>();
    
    // 会话RtpEndpoint缓存（sessionId -> {in: RtpEndpoint, out: RtpEndpoint}）
    private final Map<String, RtpEndpointPair> rtpEndpoints = new ConcurrentHashMap<>();

    /**
     * 创建媒体管道
     * 
     * @param sessionId 会话ID
     * @return MediaPipeline实例
     */
    public MediaPipeline createPipeline(String sessionId) {
        log.info("创建Kurento Pipeline: sessionId={}", sessionId);
        
        try {
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            pipelines.put(sessionId, pipeline);
            
            log.info("Pipeline创建成功: sessionId={}, pipelineId={}", 
                    sessionId, pipeline.getId());
            
            return pipeline;
        } catch (Exception e) {
            log.error("Pipeline创建失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "Kurento Pipeline创建失败: " + e.getMessage());
        }
    }

    /**
     * 创建WebRtcEndpoint
     * 
     * @param sessionId 会话ID
     * @param pipeline MediaPipeline
     * @return WebRtcEndpoint实例
     */
    public WebRtcEndpoint createWebRtcEndpoint(String sessionId, MediaPipeline pipeline) {
        log.info("创建WebRtcEndpoint: sessionId={}", sessionId);
        
        try {
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoints.put(sessionId, webRtcEndpoint);
            
            // 配置ICE候选者收集
            webRtcEndpoint.addIceCandidateFoundListener(event -> {
                log.debug("ICE候选者发现: sessionId={}, candidate={}", 
                        sessionId, event.getCandidate().getCandidate());
            });
            
            // 配置连接状态监听
            webRtcEndpoint.addConnectionStateChangedListener(event -> {
                log.info("WebRTC连接状态变更: sessionId={}, oldState={}, newState={}", 
                        sessionId, event.getOldState(), event.getNewState());
            });
            
            log.info("WebRtcEndpoint创建成功: sessionId={}, endpointId={}", 
                    sessionId, webRtcEndpoint.getId());
            
            return webRtcEndpoint;
        } catch (Exception e) {
            log.error("WebRtcEndpoint创建失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "WebRtcEndpoint创建失败: " + e.getMessage());
        }
    }

    /**
     * 创建RtpEndpoint对（用于桥接进程）
     * 
     * @param sessionId 会话ID
     * @param pipeline MediaPipeline
     * @return RtpEndpoint对（in和out）
     */
    public RtpEndpointPair createRtpEndpoints(String sessionId, MediaPipeline pipeline) {
        log.info("创建RtpEndpoint对: sessionId={}", sessionId);
        
        try {
            // 创建RtpEndpoint(in) - 接收TTS音频（从grpc2rtp-tts）
            RtpEndpoint rtpIn = new RtpEndpoint.Builder(pipeline).build();
            
            // 创建RtpEndpoint(out) - 发送ASR音频（到rtp2grpc-asr）
            RtpEndpoint rtpOut = new RtpEndpoint.Builder(pipeline).build();
            
            RtpEndpointPair pair = new RtpEndpointPair(rtpIn, rtpOut);
            rtpEndpoints.put(sessionId, pair);
            
            log.info("RtpEndpoint对创建成功: sessionId={}, inId={}, outId={}", 
                    sessionId, rtpIn.getId(), rtpOut.getId());
            
            return pair;
        } catch (Exception e) {
            log.error("RtpEndpoint对创建失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "RtpEndpoint创建失败: " + e.getMessage());
        }
    }

    /**
     * 连接媒体端点
     * 
     * @param sessionId 会话ID
     * @param webRtcEndpoint WebRTC端点
     * @param rtpEndpointPair RTP端点对
     */
    public void connectEndpoints(String sessionId, 
                                 WebRtcEndpoint webRtcEndpoint, 
                                 RtpEndpointPair rtpEndpointPair) {
        log.info("连接媒体端点: sessionId={}", sessionId);
        
        try {
            // WebRTC -> RtpOut（用户音频 -> ASR）
            webRtcEndpoint.connect(rtpEndpointPair.getOut());
            
            // RtpIn -> WebRTC（TTS -> 用户播放）
            rtpEndpointPair.getIn().connect(webRtcEndpoint);
            
            log.info("媒体端点连接成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("媒体端点连接失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "媒体端点连接失败: " + e.getMessage());
        }
    }

    /**
     * 释放会话资源
     * 
     * @param sessionId 会话ID
     */
    public void releaseSession(String sessionId) {
        log.info("释放Kurento会话资源: sessionId={}", sessionId);
        
        try {
            // 释放顺序：RtpEndpoint -> WebRtcEndpoint -> Pipeline
            
            // 1. 释放RTP端点
            RtpEndpointPair rtpPair = rtpEndpoints.remove(sessionId);
            if (rtpPair != null) {
                try {
                    rtpPair.getIn().release();
                    rtpPair.getOut().release();
                    log.debug("RtpEndpoint已释放: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.warn("RtpEndpoint释放失败: sessionId={}", sessionId, e);
                }
            }
            
            // 2. 释放WebRTC端点
            WebRtcEndpoint webRtcEndpoint = webRtcEndpoints.remove(sessionId);
            if (webRtcEndpoint != null) {
                try {
                    webRtcEndpoint.release();
                    log.debug("WebRtcEndpoint已释放: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.warn("WebRtcEndpoint释放失败: sessionId={}", sessionId, e);
                }
            }
            
            // 3. 释放Pipeline
            MediaPipeline pipeline = pipelines.remove(sessionId);
            if (pipeline != null) {
                try {
                    pipeline.release();
                    log.debug("Pipeline已释放: sessionId={}", sessionId);
                } catch (Exception e) {
                    log.warn("Pipeline释放失败: sessionId={}", sessionId, e);
                }
            }
            
            log.info("Kurento会话资源释放完成: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("释放Kurento会话资源异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 获取Pipeline
     * 
     * @param sessionId 会话ID
     * @return MediaPipeline实例
     */
    public MediaPipeline getPipeline(String sessionId) {
        return pipelines.get(sessionId);
    }

    /**
     * 获取WebRtcEndpoint
     * 
     * @param sessionId 会话ID
     * @return WebRtcEndpoint实例
     */
    public WebRtcEndpoint getWebRtcEndpoint(String sessionId) {
        return webRtcEndpoints.get(sessionId);
    }

    /**
     * 获取RtpEndpoint对
     * 
     * @param sessionId 会话ID
     * @return RtpEndpointPair实例
     */
    public RtpEndpointPair getRtpEndpoints(String sessionId) {
        return rtpEndpoints.get(sessionId);
    }

    /**
     * RtpEndpoint对（内部类）
     */
    public static class RtpEndpointPair {
        private final RtpEndpoint in;  // 接收TTS音频
        private final RtpEndpoint out; // 发送ASR音频
        
        public RtpEndpointPair(RtpEndpoint in, RtpEndpoint out) {
            this.in = in;
            this.out = out;
        }
        
        public RtpEndpoint getIn() {
            return in;
        }
        
        public RtpEndpoint getOut() {
            return out;
        }
    }
}

