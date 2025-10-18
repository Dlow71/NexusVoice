package com.nexusvoice.infrastructure.mq.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RocketMQ健康检查器
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQHealthIndicator {
    
    private final RocketMQTemplate rocketMQTemplate;
    
    /**
     * 检查RocketMQ健康状态
     * 返回健康状态信息
     */
    public RocketMQHealthStatus checkHealth() {
        RocketMQHealthStatus status = new RocketMQHealthStatus();
        
        try {
            // 检查RocketMQTemplate是否正常
            if (rocketMQTemplate == null) {
                status.setHealthy(false);
                status.setStatus("RocketMQTemplate未初始化");
                return status;
            }
            
            // 检查生产者是否正常
            boolean producerRunning = rocketMQTemplate.getProducer() != null;
            
            if (!producerRunning) {
                status.setHealthy(false);
                status.setStatus("生产者未运行");
                return status;
            }
            
            // 获取生产者信息
            status.setHealthy(true);
            status.setStatus("运行正常");
            status.setProducerGroup(rocketMQTemplate.getProducer().getProducerGroup());
            status.setSendMsgTimeout(rocketMQTemplate.getProducer().getSendMsgTimeout());
            status.setRetryTimesWhenSendFailed(rocketMQTemplate.getProducer().getRetryTimesWhenSendFailed());
            
            log.debug("RocketMQ健康检查通过");
            return status;
            
        } catch (Exception e) {
            log.error("RocketMQ健康检查失败", e);
            status.setHealthy(false);
            status.setStatus("检查失败");
            status.setErrorMessage(e.getMessage());
            return status;
        }
    }
    
    /**
     * RocketMQ健康状态
     */
    public static class RocketMQHealthStatus {
        private boolean healthy;
        private String status;
        private String producerGroup;
        private int sendMsgTimeout;
        private int retryTimesWhenSendFailed;
        private String errorMessage;
        
        // Getters and setters
        public boolean isHealthy() {
            return healthy;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getProducerGroup() {
            return producerGroup;
        }
        
        public void setProducerGroup(String producerGroup) {
            this.producerGroup = producerGroup;
        }
        
        public int getSendMsgTimeout() {
            return sendMsgTimeout;
        }
        
        public void setSendMsgTimeout(int sendMsgTimeout) {
            this.sendMsgTimeout = sendMsgTimeout;
        }
        
        public int getRetryTimesWhenSendFailed() {
            return retryTimesWhenSendFailed;
        }
        
        public void setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed) {
            this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
