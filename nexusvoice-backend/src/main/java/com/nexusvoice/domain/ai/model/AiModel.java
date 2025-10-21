package com.nexusvoice.domain.ai.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.common.BaseDomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * AI模型配置实体
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModel extends BaseDomainEntity {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 厂商代码：openai/claude/qwen等
     */
    private String providerCode;
    
    /**
     * 模型代码：gpt-4o-mini/claude-3-opus等
     */
    private String modelCode;
    
    /**
     * 模型类型：chat/embedding/rerank
     */
    private String modelType;
    
    /**
     * 模型显示名称
     */
    private String modelName;
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * LangChain4j模型类名
     * 如：OpenAiChatModel/ClaudeChatModel
     */
    private String modelClass;
    
    /**
     * 默认API端点
     */
    private String defaultBaseUrl;
    
    /**
     * 默认温度参数
     */
    private BigDecimal defaultTemperature;
    
    /**
     * 默认最大tokens
     */
    private Integer defaultMaxTokens;
    
    /**
     * 默认超时时间（秒）
     */
    private Integer defaultTimeoutSeconds;
    
    /**
     * 上下文窗口大小
     */
    private Integer contextWindow;
    
    /**
     * 输入token单价（元/千tokens）
     */
    private BigDecimal inputTokenPrice;
    
    /**
     * 输出token单价（元/千tokens）
     */
    private BigDecimal outputTokenPrice;
    
    /**
     * 额外配置JSON
     */
    private String configJson;
    
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 优先级（越小越优先）
     */
    private Integer priority;
    
    /**
     * 获取模型唯一标识
     */
    public String getModelKey() {
        return providerCode + ":" + modelCode;
    }
    
    /**
     * 计算费用
     * @param inputTokens 输入token数
     * @param outputTokens 输出token数
     * @return 总费用（元）
     */
    public BigDecimal calculateCost(int inputTokens, int outputTokens) {
        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;
        
        if (inputTokenPrice != null) {
            inputCost = inputTokenPrice.multiply(BigDecimal.valueOf(inputTokens))
                    .divide(BigDecimal.valueOf(1000), 6, java.math.RoundingMode.HALF_UP);
        }
        
        if (outputTokenPrice != null) {
            outputCost = outputTokenPrice.multiply(BigDecimal.valueOf(outputTokens))
                    .divide(BigDecimal.valueOf(1000), 6, java.math.RoundingMode.HALF_UP);
        }
        
        return inputCost.add(outputCost);
    }
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
    
    /**
     * 获取模型类型枚举
     */
    public AiModelType getModelTypeEnum() {
        return AiModelType.fromCode(modelType);
    }
    
    /**
     * 是否为对话模型
     */
    public boolean isChatModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isChat();
    }
    
    /**
     * 是否为向量模型
     */
    public boolean isEmbeddingModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isEmbedding();
    }
    
    /**
     * 是否为重排序模型
     */
    public boolean isRerankModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isRerank();
    }
    
    /**
     * 获取配置Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigMap() {
        if (configJson == null || configJson.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (Exception e) {
            log.error("解析配置JSON失败，模型：{}，配置：{}", getModelKey(), configJson, e);
            return new HashMap<>();
        }
    }
}
