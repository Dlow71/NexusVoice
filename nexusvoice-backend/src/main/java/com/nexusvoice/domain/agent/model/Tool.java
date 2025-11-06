package com.nexusvoice.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Tool工具领域实体（纯POJO）
 * 
 * 职责：
 * - 表示Agent可以使用的工具
 * - 定义工具的输入参数
 * - 描述工具的功能
 * 
 * 设计原则：
 * - 纯领域模型，无技术注解
 * - 不依赖基础设施层
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tool {
    
    /**
     * 工具名称（唯一标识）
     */
    private String name;
    
    /**
     * 工具描述（给LLM看的，描述工具功能和使用场景）
     */
    private String description;
    
    /**
     * 参数列表
     */
    private List<ToolParameter> parameters;
    
    /**
     * 是否是原子工具（不可再分解）
     */
    @Builder.Default
    private Boolean atomic = true;
    
    /**
     * 工具分类（如：search/data_processing/communication）
     */
    private String category;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private Boolean enabled = true;
    
    /**
     * 预估执行时间（毫秒）
     */
    private Long estimatedDurationMs;
    
    /**
     * 优先级（数值越大优先级越高）
     */
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * 验证工具定义是否有效
     */
    public boolean isValid() {
        return name != null && !name.isEmpty()
            && description != null && !description.isEmpty();
    }
    
    /**
     * 检查是否有必需参数
     */
    public boolean hasRequiredParameters() {
        if (parameters == null || parameters.isEmpty()) {
            return false;
        }
        return parameters.stream().anyMatch(ToolParameter::getRequired);
    }
    
    /**
     * 获取必需参数列表
     */
    public List<ToolParameter> getRequiredParameters() {
        if (parameters == null) {
            return List.of();
        }
        return parameters.stream()
            .filter(ToolParameter::getRequired)
            .toList();
    }
}

