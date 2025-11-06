package com.nexusvoice.application.agent.dto;

import com.nexusvoice.domain.agent.model.AgentStepRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent执行响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent执行响应")
public class AgentExecuteResponse {
    
    @Schema(description = "是否成功")
    private Boolean success;
    
    @Schema(description = "执行结果")
    private String result;
    
    @Schema(description = "使用的Agent名称")
    private String agentName;
    
    @Schema(description = "执行步数")
    private Integer steps;
    
    @Schema(description = "总耗时（毫秒）")
    private Long totalTimeMs;
    
    @Schema(description = "使用的工具列表")
    private List<String> usedTools;
    
    @Schema(description = "执行历史（可选，调试用）")
    private List<AgentStepRecord> executionHistory;
    
    @Schema(description = "错误信息（如果失败）")
    private String errorMessage;
}

