package com.nexusvoice.infrastructure.rtc.grpc;

import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.infrastructure.rtc.grpc.asr.*;
import com.nexusvoice.infrastructure.rtc.grpc.common.*;
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
 * ASR gRPC客户端
 * 
 * 功能：封装ASR流式识别双向流调用
 * 特性：
 * - KeepAlive机制（20s ping / 60s timeout）
 * - 自动重连与错误处理
 * - 背压支持
 * - 16MB消息大小限制
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
public class GrpcAsrClient {

    @Value("${rtc.grpc.asr.host:localhost}")
    private String asrHost;

    @Value("${rtc.grpc.asr.port:50051}")
    private int asrPort;

    @Value("${rtc.grpc.asr.max-message-size:16777216}")
    private int maxMessageSize; // 16MB

    // ASR超时设置：60秒（短语音识别，通常数秒内完成）
    @Value("${rtc.grpc.asr.timeout-seconds:60}")
    private long timeoutSeconds;

    private ManagedChannel channel;
    private AsrServiceGrpc.AsrServiceStub asyncStub;
    private AsrServiceGrpc.AsrServiceBlockingStub blockingStub;

    /**
     * 初始化gRPC通道
     */
    @PostConstruct
    public void init() {
        log.info("初始化ASR gRPC客户端: {}:{}", asrHost, asrPort);
        
        channel = ManagedChannelBuilder
                .forAddress(asrHost, asrPort)
                .usePlaintext() // MVP阶段不启用TLS
                .maxInboundMessageSize(maxMessageSize)
                .keepAliveTime(20, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();
        
        asyncStub = AsrServiceGrpc.newStub(channel);
        blockingStub = AsrServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS);
        
        log.info("ASR gRPC客户端初始化完成");
    }

    /**
     * 关闭gRPC通道
     */
    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                log.info("关闭ASR gRPC通道");
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn("ASR gRPC通道关闭中断", e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 创建流式会话
     * 
     * @param sessionId 会话ID
     * @param config ASR配置
     * @param onResult 识别结果回调
     * @param onVadEvent VAD事件回调
     * @param onError 错误回调
     * @return 请求流观察者（用于发送音频数据）
     */
    public StreamObserver<AsrRequest> createStreamSession(
            String sessionId,
            AsrConfig config,
            Consumer<RecognitionResult> onResult,
            Consumer<VadEvent> onVadEvent,
            Consumer<ErrorInfo> onError) {
        
        log.debug("创建ASR流式会话: sessionId={}", sessionId);
        
        // 响应流观察者
        StreamObserver<AsrResponse> responseObserver = new StreamObserver<AsrResponse>() {
            @Override
            public void onNext(AsrResponse response) {
                try {
                    switch (response.getResponseTypeCase()) {
                        case RESULT:
                            RecognitionResult result = response.getResult();
                            log.debug("ASR识别结果: sessionId={}, text={}, isFinal={}", 
                                    sessionId, result.getText(), result.getIsFinal());
                            onResult.accept(result);
                            break;
                            
                        case VAD_EVENT:
                            VadEvent vadEvent = response.getVadEvent();
                            log.debug("ASR VAD事件: sessionId={}, eventType={}", 
                                    sessionId, vadEvent.getEventType());
                            onVadEvent.accept(vadEvent);
                            break;
                            
                        case ERROR:
                            ErrorInfo error = response.getError();
                            log.warn("ASR错误: sessionId={}, code={}, message={}", 
                                    sessionId, error.getErrorCode(), error.getErrorMessage());
                            onError.accept(error);
                            break;
                            
                        case STATUS:
                            SessionStatus status = response.getStatus();
                            log.debug("ASR会话状态: sessionId={}, state={}", 
                                    sessionId, status.getState());
                            break;
                            
                        default:
                            log.warn("未知ASR响应类型: {}", response.getResponseTypeCase());
                    }
                } catch (Exception e) {
                    log.error("ASR响应处理异常: sessionId={}", sessionId, e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("ASR流式会话错误: sessionId={}", sessionId, t);
                ErrorInfo errorInfo = ErrorInfo.newBuilder()
                        .setErrorCode("STREAM_ERROR")
                        .setErrorMessage(t.getMessage())
                        .setTimestampMs(System.currentTimeMillis())
                        .build();
                onError.accept(errorInfo);
            }

            @Override
            public void onCompleted() {
                log.info("ASR流式会话完成: sessionId={}", sessionId);
            }
        };
        
        // 创建请求流观察者
        StreamObserver<AsrRequest> requestObserver = asyncStub.streamRecognize(responseObserver);
        
        // 发送配置消息（首消息）
        try {
            AsrRequest configRequest = AsrRequest.newBuilder()
                    .setConfig(config)
                    .build();
            requestObserver.onNext(configRequest);
            log.debug("ASR配置已发送: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("发送ASR配置失败: sessionId={}", sessionId, e);
            throw new BizException(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "ASR会话初始化失败: " + e.getMessage());
        }
        
        return requestObserver;
    }

    /**
     * 发送音频数据
     * 
     * @param requestObserver 请求流观察者
     * @param audioContent 音频内容（PCM/Opus）
     * @param sequence 序列号
     */
    public void sendAudioData(StreamObserver<AsrRequest> requestObserver, 
                              byte[] audioContent, 
                              long sequence) {
        try {
            AudioData audioData = AudioData.newBuilder()
                    .setAudioContent(com.google.protobuf.ByteString.copyFrom(audioContent))
                    .setSequence(sequence)
                    .setTimestampMs(System.currentTimeMillis())
                    .build();
            
            AsrRequest request = AsrRequest.newBuilder()
                    .setAudio(audioData)
                    .build();
            
            requestObserver.onNext(request);
        } catch (Exception e) {
            log.error("发送ASR音频数据失败: seq={}", sequence, e);
        }
    }

    /**
     * 结束会话
     * 
     * @param requestObserver 请求流观察者
     * @param reason 结束原因
     */
    public void endSession(StreamObserver<AsrRequest> requestObserver, EndReason reason) {
        try {
            EndSignal endSignal = EndSignal.newBuilder()
                    .setReason(reason)
                    .setMessage("会话正常结束")
                    .build();
            
            AsrRequest request = AsrRequest.newBuilder()
                    .setEnd(endSignal)
                    .build();
            
            requestObserver.onNext(request);
            requestObserver.onCompleted();
            
            log.info("ASR会话结束信号已发送: reason={}", reason);
        } catch (Exception e) {
            log.error("发送ASR结束信号失败", e);
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
                    .setService("asr")
                    .build();
            
            HealthCheckResponse response = blockingStub.healthCheck(request);
            
            log.debug("ASR健康检查: healthy={}, message={}", 
                    response.getHealthy(), response.getMessage());
            
            return response.getHealthy();
        } catch (Exception e) {
            log.error("ASR健康检查失败", e);
            return false;
        }
    }
}

