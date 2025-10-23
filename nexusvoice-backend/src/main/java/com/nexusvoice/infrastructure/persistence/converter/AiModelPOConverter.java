package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.infrastructure.persistence.po.AiModelPO;
import org.springframework.stereotype.Component;

/**
 * AiModel实体与AiModelPO转换器
 * 负责领域模型与持久化模型之间的转换
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Component
public class AiModelPOConverter {

    /**
     * 将领域实体转换为持久化对象
     */
    public AiModelPO toPO(AiModel model) {
        if (model == null) {
            return null;
        }

        AiModelPO po = new AiModelPO();
        
        // 基础字段
        po.setId(model.getId());
        po.setCreatedAt(model.getCreatedAt());
        po.setUpdatedAt(model.getUpdatedAt());
        po.setDeleted(model.getDeleted());
        
        // 业务字段
        po.setProviderCode(model.getProviderCode());
        po.setModelCode(model.getModelCode());
        po.setModelName(model.getModelName());
        po.setModelType(model.getModelType());
        po.setDescription(model.getDescription());
        po.setModelClass(model.getModelClass());
        po.setDefaultBaseUrl(model.getDefaultBaseUrl());
        po.setDefaultTemperature(model.getDefaultTemperature());
        po.setDefaultMaxTokens(model.getDefaultMaxTokens());
        po.setDefaultTimeoutSeconds(model.getDefaultTimeoutSeconds());
        po.setContextWindow(model.getContextWindow());
        po.setInputTokenPrice(model.getInputTokenPrice());
        po.setOutputTokenPrice(model.getOutputTokenPrice());
        po.setConfigJson(model.getConfigJson());
        po.setStatus(model.getStatus());
        po.setPriority(model.getPriority());
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public AiModel toDomain(AiModelPO po) {
        if (po == null) {
            return null;
        }

        AiModel model = new AiModel();
        
        // 基础字段
        model.setId(po.getId());
        model.setCreatedAt(po.getCreatedAt());
        model.setUpdatedAt(po.getUpdatedAt());
        model.setDeleted(po.getDeleted());
        
        // 业务字段
        model.setProviderCode(po.getProviderCode());
        model.setModelCode(po.getModelCode());
        model.setModelType(po.getModelType());
        model.setModelName(po.getModelName());
        model.setDescription(po.getDescription());
        model.setModelClass(po.getModelClass());
        model.setDefaultBaseUrl(po.getDefaultBaseUrl());
        model.setDefaultTemperature(po.getDefaultTemperature());
        model.setDefaultMaxTokens(po.getDefaultMaxTokens());
        model.setDefaultTimeoutSeconds(po.getDefaultTimeoutSeconds());
        model.setContextWindow(po.getContextWindow());
        model.setInputTokenPrice(po.getInputTokenPrice());
        model.setOutputTokenPrice(po.getOutputTokenPrice());
        model.setConfigJson(po.getConfigJson());
        model.setStatus(po.getStatus());
        model.setPriority(po.getPriority());
        
        return model;
    }
}
