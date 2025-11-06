package com.nexusvoice.application.agent.dto;

import com.nexusvoice.domain.agent.enums.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent执行请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent执行请求")
public class AgentExecuteRequest {
    
    @Schema(description = "查询内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "查询内容不能为空")
    private String query;
    
    @Schema(description = "Agent类型", example = "REACT")
    private AgentType agentType;
    
    @Schema(description = "指定Agent名称（可选）")
    private String agentName;
    
    @Schema(description = "可用工具列表（可选，不指定则使用Agent默认工具）")
    private List<String> availableTools;
    
    @Schema(description = "用户ID（自动填充）")
    private Long userId;
    
    @Schema(description = "对话ID（可选）")
    private Long conversationId;
    
    @Schema(description = "会话ID（可选）")
    private String sessionId;
    
    @Schema(description = "上下文变量（可选）")
    private Map<String, Object> contextVariables;
    
    @Schema(description = "最大执行步数（可选，默认10）")
    private Integer maxSteps;
    
    @Schema(description = "温度参数（可选，默认0.7）")
    private Double temperature;
    
    @Schema(description = "模型名称（可选，格式：provider:model，如deepseek:deepseek-v3.1）")
    private String modelName;
}

