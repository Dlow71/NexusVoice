package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * AI模型持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_models")
public class AiModelPO extends BasePO {

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
}
