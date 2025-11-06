package com.nexusvoice.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent配置值对象（纯POJO）
 * 
 * 职责：
 * - 配置Agent执行参数
 * - 控制AI模型行为
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {
    
    /**
     * 最大执行步数（防止无限循环）
     */
    @Builder.Default
    private Integer maxSteps = 10;
    
    /**
     * LLM模型名称
     */
    private String modelName;
    
    /**
     * 温度参数（0.0-1.0，控制随机性）
     */
    @Builder.Default
    private Double temperature = 0.7;
    
    /**
     * 最大Token数
     */
    @Builder.Default
    private Integer maxTokens = 2000;
    
    /**
     * 超时时间（毫秒）
     */
    @Builder.Default
    private Long timeoutMs = 60000L;
    
    /**
     * 是否启用并发工具调用
     */
    @Builder.Default
    private Boolean enableConcurrentTools = false;
    
    /**
     * 并发工具数量上限
     */
    @Builder.Default
    private Integer maxConcurrentTools = 3;
    
    /**
     * 是否启用流式输出
     */
    @Builder.Default
    private Boolean enableStreaming = false;
    
    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        return maxSteps != null && maxSteps > 0 && maxSteps <= 20
            && temperature != null && temperature >= 0.0 && temperature <= 1.0
            && maxTokens != null && maxTokens > 0
            && timeoutMs != null && timeoutMs > 0;
    }
}

