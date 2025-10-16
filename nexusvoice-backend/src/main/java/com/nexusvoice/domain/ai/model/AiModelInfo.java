package com.nexusvoice.domain.ai.model;

import com.nexusvoice.domain.ai.enums.AiProviderEnum;
import lombok.Builder;
import lombok.Data;

/**
 * AI模型信息领域模型
 * 描述一个AI模型的完整信息
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Data
@Builder
public class AiModelInfo {
    
    /**
     * 模型提供商
     */
    private AiProviderEnum provider;
    
    /**
     * 模型代码（如：gpt-oss-20b）
     */
    private String modelCode;
    
    /**
     * 模型名称（如：GPT-OSS-20B）
     */
    private String modelName;
    
    /**
     * 上下文窗口大小
     */
    private Integer contextWindow;
    
    /**
     * 最大输出令牌数
     */
    private Integer maxOutputTokens;
    
    /**
     * 是否支持工具调用
     */
    private Boolean supportTools;
    
    /**
     * 是否支持流式输出
     */
    private Boolean supportStream;
    
    /**
     * 是否支持视觉
     */
    private Boolean supportVision;
    
    /**
     * 创建模型标识符
     */
    public String getModelIdentifier() {
        return provider.getCode() + ":" + modelCode;
    }
    
    /**
     * 校验模型信息是否有效
     */
    public boolean isValid() {
        return provider != null && modelCode != null && !modelCode.trim().isEmpty();
    }
}
