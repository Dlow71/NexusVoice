package com.nexusvoice.domain.agent.model;

import com.nexusvoice.domain.agent.enums.AgentType;
import com.nexusvoice.domain.common.BaseDomainEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent领域实体（纯POJO）
 * 
 * 职责：
 * - 表示一个智能体的核心属性
 * - 定义Agent的能力边界（可用工具）
 * - 管理Agent的配置和提示词
 * 
 * 设计原则：
 * - 纯领域模型，无任何技术注解
 * - 不依赖任何基础设施层
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = false)
public class Agent extends BaseDomainEntity {
    
    /**
     * Agent名称（唯一标识）
     */
    private String name;
    
    /**
     * Agent描述
     */
    private String description;
    
    /**
     * Agent类型
     */
    private AgentType type;
    
    /**
     * 系统提示词（定义Agent的角色和行为）
     */
    private String systemPrompt;
    
    /**
     * 下一步引导提示词（引导Agent思考下一步动作）
     */
    private String nextStepPrompt;
    
    /**
     * 可用工具列表（工具名称）
     */
    private List<String> availableTools;
    
    /**
     * Agent配置
     */
    private AgentConfig config;
    
    /**
     * 所属用户ID（可选，系统Agent为null）
     */
    private Long userId;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 验证Agent是否有效
     */
    public boolean isValid() {
        return name != null && !name.isEmpty() 
            && type != null 
            && systemPrompt != null && !systemPrompt.isEmpty();
    }
    
    /**
     * 检查工具是否可用
     */
    public boolean hasToolAvailable(String toolName) {
        return availableTools != null && availableTools.contains(toolName);
    }
}

