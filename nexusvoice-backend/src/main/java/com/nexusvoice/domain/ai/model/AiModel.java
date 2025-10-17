package com.nexusvoice.domain.ai.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexusvoice.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * AI模型配置实体
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("ai_models")
public class AiModel extends BaseEntity {
    
    /**
     * 厂商代码：openai/claude/qwen等
     */
    @TableField("provider_code")
    private String providerCode;
    
    /**
     * 模型代码：gpt-4o-mini/claude-3-opus等
     */
    @TableField("model_code")
    private String modelCode;
    
    /**
     * 模型显示名称
     */
    @TableField("model_name")
    private String modelName;
    
    /**
     * 模型描述
     */
    @TableField("description")
    private String description;
    
    /**
     * LangChain4j模型类名
     * 如：OpenAiChatModel/ClaudeChatModel
     */
    @TableField("model_class")
    private String modelClass;
    
    /**
     * 默认API端点
     */
    @TableField("default_base_url")
    private String defaultBaseUrl;
    
    /**
     * 默认温度参数
     */
    @TableField("default_temperature")
    private BigDecimal defaultTemperature;
    
    /**
     * 默认最大tokens
     */
    @TableField("default_max_tokens")
    private Integer defaultMaxTokens;
    
    /**
     * 默认超时时间（秒）
     */
    @TableField("default_timeout_seconds")
    private Integer defaultTimeoutSeconds;
    
    /**
     * 上下文窗口大小
     */
    @TableField("context_window")
    private Integer contextWindow;
    
    /**
     * 输入token单价（元/千tokens）
     */
    @TableField("input_token_price")
    private BigDecimal inputTokenPrice;
    
    /**
     * 输出token单价（元/千tokens）
     */
    @TableField("output_token_price")
    private BigDecimal outputTokenPrice;
    
    /**
     * 额外配置JSON
     */
    @TableField("config_json")
    private String configJson;
    
    /**
     * 状态：0-禁用 1-启用
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 优先级（越小越优先）
     */
    @TableField("priority")
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
}
