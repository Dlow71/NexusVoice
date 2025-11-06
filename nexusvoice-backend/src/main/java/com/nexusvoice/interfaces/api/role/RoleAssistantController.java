package com.nexusvoice.interfaces.api.role;

import com.nexusvoice.annotation.RequireUser;
import com.nexusvoice.application.agent.dto.AgentExecuteRequest;
import com.nexusvoice.application.agent.dto.AgentExecuteResponse;
import com.nexusvoice.application.agent.service.AgentApplicationService;
import com.nexusvoice.application.role.dto.RoleAssistantConfirmRequest;
import com.nexusvoice.application.role.dto.RoleBriefDto;
import com.nexusvoice.application.role.dto.RoleResearchTaskPreviewDto;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleResearchApplyRequest;
import com.nexusvoice.application.role.service.RoleAssistantService;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.agent.model.Tool;
import com.nexusvoice.domain.agent.repository.ToolRegistry;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

/**
 * 角色助手API
 * - 从对话生成角色草稿
 * - 确认创建私人角色
 * - 通用Agent执行接口
 */
@RestController
@RequestMapping("/api/roles/assistant")
@Tag(name = "角色助手", description = "基于对话创建AI角色")
public class RoleAssistantController {

    private final RoleAssistantService roleAssistantService;
    private final AgentApplicationService agentApplicationService;  // 新增
    private final ToolRegistry toolRegistry;  // 新增

    public RoleAssistantController(RoleAssistantService roleAssistantService,
                                   AgentApplicationService agentApplicationService,
                                   ToolRegistry toolRegistry) {
        this.roleAssistantService = roleAssistantService;
        this.agentApplicationService = agentApplicationService;
        this.toolRegistry = toolRegistry;
    }

    @GetMapping("/research/tasks")
    @RequireUser
    @Operation(summary = "预览深研任务清单", description = "基于最近生成的角色草稿，给出建议的检索任务列表（可在前端删改再提交）")
    public Result<RoleResearchTaskPreviewDto> previewTasks(
            @Parameter(description = "对话ID", required = true) @RequestParam("conversationId") @NotNull Long conversationId
    ) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        RoleResearchTaskPreviewDto preview = roleAssistantService.previewResearchTasks(conversationId, userId);
        return Result.success("获取成功", preview);
    }

    @PostMapping("/research/apply")
    @RequireUser
    @Operation(summary = "应用深研任务并更新草稿", description = "根据用户编辑的任务（查询集合）执行深研，只更新角色草稿，不创建角色")
    public Result<RoleBriefDto> applyResearch(@jakarta.validation.Valid @RequestBody RoleResearchApplyRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        RoleBriefDto updated = roleAssistantService.applyResearchAndUpdateBrief(request, userId);
        return Result.success("已更新草稿", updated);
    }

    @PostMapping("/brief")
    @RequireUser
    @Operation(summary = "从对话生成角色草稿", description = "读取对话上下文，生成可预览的角色草稿（快速模式，支持按需联网）")
    public Result<RoleBriefDto> generateBrief(
            @Parameter(description = "对话ID", required = true) @RequestParam("conversationId") @NotNull Long conversationId,
            @Parameter(description = "是否启用联网搜索", example = "false") @RequestParam(value = "enableWebSearch", required = false, defaultValue = "false") boolean enableWebSearch
    ) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        RoleBriefDto brief = roleAssistantService.generateBriefFromConversation(conversationId, userId, enableWebSearch);
        return Result.success("生成成功", brief);
    }

    @PostMapping("/confirm")
    @RequireUser
    @Operation(summary = "确认创建私人角色（Agent增强版）", 
               description = "使用最近一次草稿直接创建私人角色；可选开启深研增强（由Agent智能执行），可自定义头像URL")
    public Result<RoleDTO> confirmCreate(@jakarta.validation.Valid @RequestBody RoleAssistantConfirmRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        RoleDTO dto = roleAssistantService.confirmCreateRole(request, userId);
        return Result.success("创建成功", dto);
    }
    
    /**
     * 新增：通用Agent执行接口
     */
    @PostMapping("/agent/execute")
    @RequireUser
    @Operation(summary = "执行Agent任务", 
               description = "通用的Agent任务执行接口，支持ReAct模式，可用于各种智能任务处理")
    public Result<AgentExecuteResponse> executeAgentTask(
            @jakarta.validation.Valid @RequestBody AgentExecuteRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        request.setUserId(userId);
        
        AgentExecuteResponse response = agentApplicationService.executeTask(request);
        
        if (response.getSuccess()) {
            return Result.success("执行成功", response);
        } else {
            return Result.<AgentExecuteResponse>error(500, response.getErrorMessage()).setData(response);
        }
    }
    
    /**
     * 新增：获取可用工具列表
     */
    @GetMapping("/tools")
    @Operation(summary = "获取可用工具列表", description = "返回系统中注册的所有工具，包括工具描述和参数定义")
    public Result<java.util.List<ToolDTO>> getAvailableTools() {
        java.util.List<Tool> tools = toolRegistry.getEnabledTools();
        
        // 转换为DTO
        java.util.List<ToolDTO> toolDTOs = tools.stream()
            .map(tool -> ToolDTO.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .category(tool.getCategory())
                .parameters(tool.getParameters())
                .estimatedDurationMs(tool.getEstimatedDurationMs())
                .priority(tool.getPriority())
                .build())
            .toList();
        
        return Result.success("获取成功", toolDTOs);
    }
    
    /**
     * 工具DTO（内部类）
     */
    @lombok.Data
    @lombok.Builder
    @io.swagger.v3.oas.annotations.media.Schema(description = "工具信息")
    private static class ToolDTO {
        @io.swagger.v3.oas.annotations.media.Schema(description = "工具名称")
        private String name;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "工具描述")
        private String description;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "工具分类")
        private String category;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "参数定义")
        private java.util.List<com.nexusvoice.domain.agent.model.ToolParameter> parameters;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "预估执行时间（毫秒）")
        private Long estimatedDurationMs;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "优先级")
        private Integer priority;
    }
}
