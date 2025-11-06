package com.nexusvoice.infrastructure.rtc.grpc;

import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.infrastructure.rtc.grpc.common.*;
import com.nexusvoice.infrastructure.rtc.grpc.tts.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TTS gRPC客户端
 * 
 * 功能：封装TTS流式合成双向流调用
 * 特性：
 * - KeepAlive机制（20s ping / 60s timeout）
 * - 自动重连与错误处理
 * - 背压支持
 * - 32MB消息大小限制（支持大音频）
 * - 静音帧注入支持
 * - 条件化加载（仅当rtc.enabled=true时生效）
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "rtc.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class GrpcTtsClient {

    @Value("${rtc.grpc.tts.host:localhost}")
    private String ttsHost;

    @Value("${rtc.grpc.tts.port:50052}")
    private int ttsPort;

    @Value("${rtc.grpc.tts.max-message-size:33554432}")
    private int maxMessageSize; // 32MB

    // TTS超时设置：120秒（长文本合成需要更长时间，是ASR的2倍）
    @Value("${rtc.grpc.tts.timeout-seconds:120}")
    private long timeoutSeconds;

    private ManagedChannel channel;
    private TtsServiceGrpc.TtsServiceStub asyncStub;
    private TtsServiceGrpc.TtsServiceBlockingStub blockingStub;

    /**
     * 初始化gRPC通道
     */
    @PostConstruct
    public void init() {
        log.info("初始化TTS gRPC客户端: {}:{}", ttsHost, ttsPort);
        
        channel = ManagedChannelBuilder
                .forAddress(ttsHost, ttsPort)
                .usePlaintext() // MVP阶段不启用TLS
                .maxInboundMessageSize(maxMessageSize)
                .keepAliveTime(20, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();
        
        asyncStub = TtsServiceGrpc.newStub(channel);
        blockingStub = TtsServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS);
        
        log.info("TTS gRPC客户端初始化完成");
    }

    /**
     * 关闭gRPC通道
     */
    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                log.info("关闭TTS gRPC通道");
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("TTS gRPC通道关闭中断", e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 创建流式会话
     * 
     * @param sessionId 会话ID
     * @param config TTS配置
     * @param onAudioData 音频数据回调
     * @param onProgress 进度回调
     * @param onError 错误回调
     * @return 请求流观察者（用于发送文本数据）
     */
    public StreamObserver<TtsRequest> createStreamSession(
            String sessionId,
            TtsConfig config,
            Consumer<TtsAudioData> onAudioData,
            Consumer<SynthesisProgress> onProgress,
            Consumer<ErrorInfo> onError) {
        
        log.debug("创建TTS流式会话: sessionId={}", sessionId);
        
        // 响应流观察者
        StreamObserver<TtsResponse> responseObserver = new StreamObserver<TtsResponse>() {
            @Override
            public void onNext(TtsResponse response) {
                try {
                    switch (response.getResponseTypeCase()) {
                        case AUDIO:
                            TtsAudioData audioData = response.getAudio();
                            log.debug("TTS音频数据: sessionId={}, segmentId={}, sequence={}, isSilence={}", 
                                    sessionId, audioData.getSegmentId(), audioData.getSequence(), audioData.getIsSilence());
                            onAudioData.accept(audioData);
                            break;
                            
                        case PROGRESS:
                            SynthesisProgress progress = response.getProgress();
                            log.debug("TTS合成进度: sessionId={}, segmentId={}, progress={}%", 
                                    sessionId, progress.getSegmentId(), progress.getProgressPercentage());
                            onProgress.accept(progress);
                            break;
                            
                        case ERROR:
                            ErrorInfo error = response.getError();
                            log.warn("TTS错误: sessionId={}, code={}, message={}", 
                                    sessionId, error.getErrorCode(), error.getErrorMessage());
                            onError.accept(error);
                            break;
                            
                        case STATUS:
                            SessionStatus status = response.getStatus();
                            log.debug("TTS会话状态: sessionId={}, state={}", 
                                    sessionId, status.getState());
                            break;
                            
                        default:
                            log.warn("未知TTS响应类型: {}", response.getResponseTypeCase());
                    }
                } catch (Exception e) {
                    log.error("TTS响应处理异常: sessionId={}", sessionId, e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("TTS流式会话错误: sessionId={}", sessionId, t);
                ErrorInfo errorInfo = ErrorInfo.newBuilder()
                        .setErrorCode("STREAM_ERROR")
                        .setErrorMessage(t.getMessage())
                        .setTimestampMs(System.currentTimeMillis())
                        .build();
                onError.accept(errorInfo);
            }

            @Override
            public void onCompleted() {
                log.info("TTS流式会话完成: sessionId={}", sessionId);
            }
        };
        
        // 创建请求流观察者
        StreamObserver<TtsRequest> requestObserver = asyncStub.streamSynthesize(responseObserver);
        
        // 发送配置消息（首消息）
        try {
            TtsRequest configRequest = TtsRequest.newBuilder()
                    .setConfig(config)
                    .build();
            requestObserver.onNext(configRequest);
            log.debug("TTS配置已发送: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("发送TTS配置失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "TTS会话初始化失败: " + e.getMessage());
        }
        
        return requestObserver;
    }

    /**
     * 发送文本数据
     * 
     * @param requestObserver 请求流观察者
     * @param text 文本内容
     * @param segmentId 片段ID
     * @param isLast 是否最后片段
     */
    public void sendTextData(StreamObserver<TtsRequest> requestObserver, 
                             String text, 
                             int segmentId, 
                             boolean isLast) {
        try {
            TextData textData = TextData.newBuilder()
                    .setText(text)
                    .setSegmentId(segmentId)
                    .setIsLast(isLast)
                    .setTimestampMs(System.currentTimeMillis())
                    .setIsSsml(false)
                    .build();
            
            TtsRequest request = TtsRequest.newBuilder()
                    .setText(textData)
                    .build();
            
            requestObserver.onNext(request);
            log.debug("TTS文本数据已发送: segmentId={}, text={}", segmentId, text);
        } catch (Exception e) {
            log.error("发送TTS文本数据失败: segmentId={}", segmentId, e);
        }
    }

    /**
     * 发送打断信号（服务端应停止合成）
     * 
     * @param requestObserver 请求流观察者
     */
    public void sendInterrupt(StreamObserver<TtsRequest> requestObserver) {
        try {
            EndSignal endSignal = EndSignal.newBuilder()
                    .setReason(EndReason.USER_INTERRUPT)
                    .setMessage("用户打断")
                    .build();
            
            TtsRequest request = TtsRequest.newBuilder()
                    .setEnd(endSignal)
                    .build();
            
            requestObserver.onNext(request);
            log.info("TTS打断信号已发送");
        } catch (Exception e) {
            log.error("发送TTS打断信号失败", e);
        }
    }

    /**
     * 结束会话
     * 
     * @param requestObserver 请求流观察者
     * @param reason 结束原因
     */
    public void endSession(StreamObserver<TtsRequest> requestObserver, EndReason reason) {
        try {
            EndSignal endSignal = EndSignal.newBuilder()
                    .setReason(reason)
                    .setMessage("会话正常结束")
                    .build();
            
            TtsRequest request = TtsRequest.newBuilder()
                    .setEnd(endSignal)
                    .build();
            
            requestObserver.onNext(request);
            requestObserver.onCompleted();
            
            log.info("TTS会话结束信号已发送: reason={}", reason);
        } catch (Exception e) {
            log.error("发送TTS结束信号失败", e);
        }
    }

    /**
     * 健康检查
     * 
     * @return 是否健康
     */
    public boolean healthCheck() {
        try {
            HealthCheckRequest request = HealthCheckRequest.newBuilder()
                    .setService("tts")
                    .build();
            
            HealthCheckResponse response = blockingStub.healthCheck(request);
            
            log.debug("TTS健康检查: healthy={}, message={}", 
                    response.getHealthy(), response.getMessage());
            
            return response.getHealthy();
        } catch (Exception e) {
            log.error("TTS健康检查失败", e);
            return false;
        }
    }
}

