package com.nexusvoice.interfaces.api.conversation;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ChatResponseDto;
import com.nexusvoice.application.conversation.dto.ConversationListDto;
import com.nexusvoice.application.conversation.dto.ConversationCreateRequest;
import com.nexusvoice.application.conversation.dto.ConversationCreateResponse;
import com.nexusvoice.application.conversation.dto.ConversationMessageWithRoleDto;
import com.nexusvoice.application.conversation.service.ConversationApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.utils.SecurityUtils;
import com.nexusvoice.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 对话相关API控制器
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "对话管理", description = "AI对话相关接口")
@Validated
public class ConversationController {

    private final ConversationApplicationService conversationApplicationService;
    private final JwtUtils jwtUtils;

    public ConversationController(ConversationApplicationService conversationApplicationService, JwtUtils jwtUtils) {
        this.conversationApplicationService = conversationApplicationService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping
    @RequireAuth
    @Operation(summary = "创建新对话", description = "创建一个新的对话并返回对话ID。后续发送消息时传入conversationId即可继续该会话，不会重复创建。")
    public Result<ConversationCreateResponse> createConversation(@Valid @RequestBody ConversationCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        log.info("创建新对话请求，用户ID：{}，标题：{}，模型：{}", userId, request.getTitle(), request.getModelName());

        ConversationCreateResponse response = conversationApplicationService.createConversation(request, userId);
        return Result.success(response);
    }



    @PostMapping("/chat")
    @RequireAuth
    @Operation(summary = "发送聊天消息", description = "向AI发送消息并获取回复，支持新建对话或在现有对话中继续。可通过enableWebSearch参数控制是否启用联网搜索功能（默认为false）")
    public Result<ChatResponseDto> chat(@Valid @RequestBody ChatRequestDto request) {
        // 获取用户ID
        Long currentUserId = SecurityUtils.getCurrentUserId().get();
        log.info("用户发起聊天请求，用户ID：{}，对话ID：{}，联网搜索：{}",
                currentUserId, request.getConversationId(),
                request.getEnableWebSearch() != null ? request.getEnableWebSearch() : false);
        
        ChatResponseDto response = conversationApplicationService.chat(request, currentUserId);
        
        if (response.getSuccess()) {
            log.info("聊天成功，用户ID：{}，对话ID：{}，响应时间：{}ms", 
                    currentUserId, response.getConversationId(), response.getResponseTimeMs());
            return Result.success(response);
        } else {
            log.warn("聊天失败，用户ID：{}，错误：{}", currentUserId, response.getErrorMessage());
            return Result.error(response.getErrorMessage());
        }
    }

    @GetMapping("/list")
    @RequireAuth
    @Operation(summary = "获取对话列表", description = "获取当前用户的对话列表，按最后活跃时间倒序")
    public Result<List<ConversationListDto>> getConversationList(
            @Parameter(description = "返回数量限制", example = "20")
            @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        log.info("获取对话列表，用户ID：{}，限制数量：{}", userId, limit);
        
        List<ConversationListDto> conversations = conversationApplicationService.getUserConversations(userId, limit);
        
        return Result.success(conversations);
    }

    @GetMapping("/{conversationId}/history")
    @RequireAuth
    @Operation(summary = "获取对话历史", description = "获取指定对话的完整消息历史")
    public Result<List<ConversationMessageWithRoleDto>> getConversationHistory(
            @Parameter(description = "对话ID", example = "1")
            @PathVariable Long conversationId) {

        Long userId = SecurityUtils.getCurrentUserId().get();
        log.info("获取对话历史，用户ID：{}，对话ID：{}", userId, conversationId);
        
        List<ConversationMessageWithRoleDto> history = conversationApplicationService.getConversationHistory(conversationId, userId);
        
        return Result.success(history);
    }

    @DeleteMapping("/{conversationId}")
    @RequireAuth
    @Operation(summary = "删除对话", description = "删除指定的对话（逻辑删除）")
    public Result<Void> deleteConversation(
            @Parameter(description = "对话ID", example = "1")
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        
        // 从JWT中获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId().get();
        log.info("删除对话，用户ID：{}，对话ID：{}", userId, conversationId);
        
        conversationApplicationService.deleteConversation(conversationId, userId);
        
        return Result.success();
    }

    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表", description = "获取系统支持的AI模型列表")
    public Result<List<String>> getAvailableModels() {
        // 返回支持的模型列表
        List<String> models = List.of(
                "gpt-4o-mini", 
                "gpt-4o", 
                "gpt-4-turbo", 
                "gpt-3.5-turbo"
        );
        
        return Result.success(models);
    }
}
