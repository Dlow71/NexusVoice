package com.nexusvoice.interfaces.api.role;

import com.nexusvoice.annotation.RequireUser;
import com.nexusvoice.application.role.dto.RoleAssistantConfirmRequest;
import com.nexusvoice.application.role.dto.RoleBriefDto;
import com.nexusvoice.application.role.dto.RoleResearchTaskPreviewDto;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleResearchApplyRequest;
import com.nexusvoice.application.role.service.RoleAssistantService;
import com.nexusvoice.common.Result;
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
 */
@RestController
@RequestMapping("/api/roles/assistant")
@Tag(name = "角色助手", description = "基于对话创建AI角色")
public class RoleAssistantController {

    private final RoleAssistantService roleAssistantService;

    public RoleAssistantController(RoleAssistantService roleAssistantService) {
        this.roleAssistantService = roleAssistantService;
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
    @Operation(summary = "确认创建私人角色", description = "使用最近一次草稿直接创建私人角色；可选开启深研增强，可自定义头像URL")
    public Result<RoleDTO> confirmCreate(@jakarta.validation.Valid @RequestBody RoleAssistantConfirmRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        RoleDTO dto = roleAssistantService.confirmCreateRole(request, userId);
        return Result.success("创建成功", dto);
    }
}
