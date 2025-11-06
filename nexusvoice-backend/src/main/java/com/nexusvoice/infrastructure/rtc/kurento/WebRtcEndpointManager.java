package com.nexusvoice.infrastructure.rtc.kurento;

import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Continuation;
import org.kurento.client.IceCandidate;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * WebRTC端点管理器
 * 
 * 功能：管理WebRTC端点的SDP协商和ICE候选者处理
 * 职责：
 * - SDP Offer/Answer协商
 * - ICE候选者收集和添加
 * - 端点释放
 * - 连接状态监控
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
public class WebRtcEndpointManager {

    private final KurentoMediaService kurentoMediaService;
    
    // ICE候选者缓存（sessionId -> List<IceCandidate>）
    private final Map<String, java.util.List<IceCandidate>> iceCandidatesCache = new ConcurrentHashMap<>();

    /**
     * 处理SDP Offer并生成Answer
     * 
     * @param sessionId 会话ID
     * @param sdpOffer SDP Offer
     * @return SDP Answer
     */
    public String processOffer(String sessionId, String sdpOffer) {
        log.info("处理SDP Offer: sessionId={}", sessionId);
        
        WebRtcEndpoint webRtcEndpoint = kurentoMediaService.getWebRtcEndpoint(sessionId);
        if (webRtcEndpoint == null) {
            log.error("WebRtcEndpoint不存在: sessionId={}", sessionId);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "WebRTC端点不存在");
        }
        
        try {
            // 处理Offer并生成Answer（同步调用）
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
            
            // 开始收集ICE候选者
            webRtcEndpoint.gatherCandidates();
            
            // 添加缓存的ICE候选者（如果有）
            addCachedIceCandidates(sessionId);
            
            log.info("SDP Answer生成成功: sessionId={}", sessionId);
            // 不打印完整SDP内容，避免泄露IP等敏感信息
            if (log.isDebugEnabled()) {
                log.debug("SDP Answer前100字符: {}", 
                        sdpAnswer != null && sdpAnswer.length() > 100 ? sdpAnswer.substring(0, 100) : sdpAnswer);
            }
            
            return sdpAnswer;
        } catch (Exception e) {
            log.error("SDP协商失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, 
                    "SDP协商失败: " + e.getMessage());
        }
    }

    /**
     * 处理SDP Offer并异步生成Answer
     * 
     * @param sessionId 会话ID
     * @param sdpOffer SDP Offer
     * @return CompletableFuture<SDP Answer>
     */
    public CompletableFuture<String> processOfferAsync(String sessionId, String sdpOffer) {
        log.info("异步处理SDP Offer: sessionId={}", sessionId);
        
        WebRtcEndpoint webRtcEndpoint = kurentoMediaService.getWebRtcEndpoint(sessionId);
        if (webRtcEndpoint == null) {
            log.error("WebRtcEndpoint不存在: sessionId={}", sessionId);
            return CompletableFuture.failedFuture(
                    new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "WebRTC端点不存在"));
        }
        
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            webRtcEndpoint.processOffer(sdpOffer, new Continuation<String>() {
                @Override
                public void onSuccess(String sdpAnswer) {
                    log.info("异步SDP Answer生成成功: sessionId={}", sessionId);
                    
                    // 开始收集ICE候选者
                    webRtcEndpoint.gatherCandidates();
                    
                    future.complete(sdpAnswer);
                }
                
                @Override
                public void onError(Throwable error) {
                    log.error("异步SDP协商失败: sessionId={}", sessionId, error);
                    future.completeExceptionally(error);
                }
            });
        } catch (Exception e) {
            log.error("启动异步SDP协商失败: sessionId={}", sessionId, e);
            future.completeExceptionally(e);
        }
        
        return future;
    }

    /**
     * 添加ICE候选者
     * 
     * @param sessionId 会话ID
     * @param candidate ICE候选者字符串
     * @param sdpMid SDP媒体ID
     * @param sdpMLineIndex SDP媒体行索引
     */
    public void addIceCandidate(String sessionId, String candidate, String sdpMid, int sdpMLineIndex) {
        log.debug("添加ICE候选者: sessionId={}, candidate={}", sessionId, candidate);
        
        WebRtcEndpoint webRtcEndpoint = kurentoMediaService.getWebRtcEndpoint(sessionId);
        if (webRtcEndpoint == null) {
            log.warn("WebRtcEndpoint不存在，缓存ICE候选者: sessionId={}", sessionId);
            
            // 缓存候选者，等待端点创建后再添加
            IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);
            iceCandidatesCache.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>())
                    .add(iceCandidate);
            return;
        }
        
        try {
            IceCandidate iceCandidate = new IceCandidate(candidate, sdpMid, sdpMLineIndex);
            webRtcEndpoint.addIceCandidate(iceCandidate);
            
            log.debug("ICE候选者添加成功: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("ICE候选者添加失败: sessionId={}", sessionId, e);
        }
    }

    /**
     * 批量添加缓存的ICE候选者
     * 
     * @param sessionId 会话ID
     */
    public void addCachedIceCandidates(String sessionId) {
        java.util.List<IceCandidate> cachedCandidates = iceCandidatesCache.remove(sessionId);
        if (cachedCandidates == null || cachedCandidates.isEmpty()) {
            return;
        }
        
        log.info("添加缓存的ICE候选者: sessionId={}, count={}", sessionId, cachedCandidates.size());
        
        WebRtcEndpoint webRtcEndpoint = kurentoMediaService.getWebRtcEndpoint(sessionId);
        if (webRtcEndpoint == null) {
            log.warn("WebRtcEndpoint不存在，无法添加缓存候选者: sessionId={}", sessionId);
            return;
        }
        
        for (IceCandidate candidate : cachedCandidates) {
            try {
                webRtcEndpoint.addIceCandidate(candidate);
            } catch (Exception e) {
                log.error("添加缓存ICE候选者失败: sessionId={}", sessionId, e);
            }
        }
    }

    /**
     * 等待ICE连接建立
     * 
     * @param sessionId 会话ID
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否连接成功
     */
    public boolean waitForIceConnection(String sessionId, int timeoutSeconds) {
        log.info("等待ICE连接建立: sessionId={}, timeout={}s", sessionId, timeoutSeconds);
        
        WebRtcEndpoint webRtcEndpoint = kurentoMediaService.getWebRtcEndpoint(sessionId);
        if (webRtcEndpoint == null) {
            log.error("WebRtcEndpoint不存在: sessionId={}", sessionId);
            return false;
        }
        
        CompletableFuture<Boolean> connectionFuture = new CompletableFuture<>();
        
        // 监听连接状态变更
        webRtcEndpoint.addConnectionStateChangedListener(event -> {
            String newState = event.getNewState().name();
            log.info("WebRTC连接状态: sessionId={}, state={}", sessionId, newState);
            
            if ("CONNECTED".equals(newState)) {
                connectionFuture.complete(true);
            } else if ("FAILED".equals(newState) || "DISCONNECTED".equals(newState)) {
                connectionFuture.complete(false);
            }
        });
        
        try {
            return connectionFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("等待连接超时: sessionId={}", sessionId, e);
            return false;
        }
    }

    /**
     * 获取ICE候选者
     * 
     * @param sessionId 会话ID
     * @return ICE候选者列表
     */
    public java.util.List<IceCandidate> getIceCandidates(String sessionId) {
        WebRtcEndpoint webRtcEndpoint = kurentoMediaService.getWebRtcEndpoint(sessionId);
        if (webRtcEndpoint == null) {
            log.warn("WebRtcEndpoint不存在: sessionId={}", sessionId);
            return java.util.Collections.emptyList();
        }
        
        try {
            // Kurento会通过事件回调ICE候选者，这里返回空列表
            // 实际候选者通过IceCandidateFoundListener获取
            return java.util.Collections.emptyList();
        } catch (Exception e) {
            log.error("获取ICE候选者失败: sessionId={}", sessionId, e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 释放端点
     * 
     * @param sessionId 会话ID
     */
    public void releaseEndpoint(String sessionId) {
        log.info("释放WebRTC端点: sessionId={}", sessionId);
        
        // 清理缓存的ICE候选者
        iceCandidatesCache.remove(sessionId);
        
        // 通过KurentoMediaService统一释放
        kurentoMediaService.releaseSession(sessionId);
    }
}

