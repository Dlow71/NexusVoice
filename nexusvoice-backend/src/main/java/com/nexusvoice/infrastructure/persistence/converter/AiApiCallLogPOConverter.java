package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.infrastructure.persistence.po.AiApiCallLogPO;
import org.springframework.stereotype.Component;

/**
 * AiApiCallLog实体与AiApiCallLogPO转换器
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Component
public class AiApiCallLogPOConverter {

    public AiApiCallLogPO toPO(AiApiCallLog log) {
        if (log == null) {
            return null;
        }

        AiApiCallLogPO po = new AiApiCallLogPO();
        
        // 基础字段
        po.setId(log.getId());
        po.setCreatedAt(log.getCreatedAt());
        po.setUpdatedAt(log.getUpdatedAt());
        // 注意：日志表没有deleted字段
        
        // 业务字段
        po.setApiKeyId(log.getApiKeyId());
        po.setDeveloperApiKeyId(log.getDeveloperApiKeyId());
        po.setAuthType(log.getAuthType());
        po.setProviderCode(log.getProviderCode());
        po.setModelCode(log.getModelCode());
        po.setUserId(log.getUserId());
        po.setConversationId(log.getConversationId());
        po.setRequestId(log.getRequestId());
        po.setRequestTime(log.getRequestTime());
        po.setRequestParams(log.getRequestParams());
        po.setResponseTime(log.getResponseTime());
        po.setResponseTimeMs(log.getResponseTimeMs());
        po.setStatus(log.getStatus());
        po.setErrorMessage(log.getErrorMessage());
        po.setPromptTokens(log.getPromptTokens());
        po.setCompletionTokens(log.getCompletionTokens());
        po.setTotalTokens(log.getTotalTokens());
        po.setInputCost(log.getInputCost());
        po.setOutputCost(log.getOutputCost());
        po.setTotalCost(log.getTotalCost());
        
        return po;
    }

    public AiApiCallLog toDomain(AiApiCallLogPO po) {
        if (po == null) {
            return null;
        }

        AiApiCallLog log = new AiApiCallLog();
        
        // 基础字段
        log.setId(po.getId());
        log.setCreatedAt(po.getCreatedAt());
        log.setUpdatedAt(po.getUpdatedAt());
        log.setDeleted(0); // 日志表没有deleted字段，默认为0
        
        // 业务字段
        log.setApiKeyId(po.getApiKeyId());
        log.setDeveloperApiKeyId(po.getDeveloperApiKeyId());
        log.setAuthType(po.getAuthType());
        log.setProviderCode(po.getProviderCode());
        log.setModelCode(po.getModelCode());
        log.setUserId(po.getUserId());
        log.setConversationId(po.getConversationId());
        log.setRequestId(po.getRequestId());
        log.setRequestTime(po.getRequestTime());
        log.setRequestParams(po.getRequestParams());
        log.setResponseTime(po.getResponseTime());
        log.setResponseTimeMs(po.getResponseTimeMs());
        log.setStatus(po.getStatus());
        log.setErrorMessage(po.getErrorMessage());
        log.setPromptTokens(po.getPromptTokens());
        log.setCompletionTokens(po.getCompletionTokens());
        log.setTotalTokens(po.getTotalTokens());
        log.setInputCost(po.getInputCost());
        log.setOutputCost(po.getOutputCost());
        log.setTotalCost(po.getTotalCost());
        
        return log;
    }
}
