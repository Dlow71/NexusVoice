package com.nexusvoice.interfaces.api.conversation;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.conversation.dto.ChatRequestDto;
import com.nexusvoice.application.conversation.dto.ChatResponseDto;
import com.nexusvoice.application.conversation.dto.ConversationListDto;
import com.nexusvoice.application.conversation.dto.ConversationCreateRequest;
import com.nexusvoice.application.conversation.dto.ConversationCreateResponse;
import com.nexusvoice.application.conversation.dto.ConversationMessageWithRoleDto;
import com.nexusvoice.application.conversation.dto.ConversationRuntimeConfigDto;
import com.nexusvoice.application.conversation.dto.ConversationRuntimePolicyDto;
import com.nexusvoice.application.conversation.service.ConversationApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
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
    private final com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager modelBeanManager;

    public ConversationController(ConversationApplicationService conversationApplicationService, 
                                 JwtUtils jwtUtils,
                                 com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager modelBeanManager) {
        this.conversationApplicationService = conversationApplicationService;
        this.jwtUtils = jwtUtils;
        this.modelBeanManager = modelBeanManager;
    }

    @PostMapping
    @RequireAuth
    @Operation(summary = "创建新对话", description = "创建一个新的对话并返回对话ID。后续发送消息时传入conversationId即可继续该会话，不会重复创建。可通过enableAudio参数控制是否生成角色开场白音频（默认为false）")
    public Result<ConversationCreateResponse> createConversation(@Valid @RequestBody ConversationCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        log.info("创建新对话请求，用户ID：{}，标题：{}，模型：{}", userId, request.getTitle(), request.getModelName());

        ConversationCreateResponse response = conversationApplicationService.createConversation(request, userId);
        return Result.success(response);
    }



    @PostMapping("/chat")
    @RequireAuth
    @Operation(summary = "发送聊天消息", description = "向AI发送消息并获取回复，支持新建对话或在现有对话中继续。可通过enableWebSearch参数控制是否启用联网搜索功能（默认为false），可通过enableAudio参数控制是否生成回复音频（默认为false）")
    public Result<ChatResponseDto> chat(@Valid @RequestBody ChatRequestDto request) {
        // 获取用户ID
        Long currentUserId = SecurityUtils.getCurrentUserId().get();
        log.info("用户发起聊天请求，用户ID：{}，对话ID：{}，联网搜索：{}，语音合成：{}",
                currentUserId, request.getConversationId(),
                request.getEnableWebSearch() != null ? request.getEnableWebSearch() : false,
                request.getEnableAudio() != null ? request.getEnableAudio() : false);
        
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

    @GetMapping("/{conversationId}/runtime-config")
    @RequireAuth
    @Operation(summary = "获取会话运行配置", description = "获取指定对话的高级运行设置与当前上下文快照")
    public Result<ConversationRuntimeConfigDto> getConversationRuntimeConfig(
            @Parameter(description = "对话ID", example = "1")
            @PathVariable Long conversationId) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        return Result.success(conversationApplicationService.getConversationRuntimeConfig(conversationId, userId));
    }

    @PutMapping("/{conversationId}/runtime-config")
    @RequireAuth
    @Operation(summary = "更新会话运行配置", description = "更新指定对话的高级运行设置，如温度、上下文策略、compact阈值等")
    public Result<ConversationRuntimeConfigDto> updateConversationRuntimeConfig(
            @Parameter(description = "对话ID", example = "1")
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationRuntimePolicyDto request) {
        Long userId = SecurityUtils.getCurrentUserId().get();
        return Result.success(conversationApplicationService.updateConversationRuntimeConfig(conversationId, request, userId));
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
    @Operation(summary = "获取可用模型列表", description = "获取系统支持的AI模型列表（从数据库动态加载）")
    public Result<List<ModelInfo>> getAvailableModels() {
        // 从动态模型管理器获取可用模型列表
        List<com.nexusvoice.domain.ai.model.AiModel> models = modelBeanManager.getAvailableModels();
        
        // 转换为前端友好的格式，包含模型能力信息
        List<ModelInfo> modelInfos = models.stream()
                .map(model -> ModelInfo.builder()
                        .modelKey(model.getModelKey()) // provider:model格式
                        .modelName(model.getModelName())
                        .description(model.getDescription())
                        .providerCode(model.getProviderCode())
                        .modelCode(model.getModelCode())
                        .contextWindow(model.getContextWindow())
                        // 能力字段
                        .supportsOcr(model.supportsOcr())
                        .supportsVision(model.supportsVision())
                        .supportsThinking(model.supportsThinking())
                        .supportsToolCall(model.supportsToolCall())
                        .supportsImageInput(model.supportsImageInput())
                        .supportsVideoInput(model.supportsVideoInput())
                        .supportsAudioInput(model.supportsAudioInput())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        log.info("返回可用模型列表，共{}个模型", modelInfos.size());
        return Result.success(modelInfos);
    }
    
    /**
     * 模型信息内部类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModelInfo {
        @io.swagger.v3.oas.annotations.media.Schema(description = "模型键值（格式：provider:model）", example = "openai:gpt-4o-mini")
        private String modelKey;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "模型名称", example = "GPT-4o Mini")
        private String modelName;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "模型描述", example = "OpenAI GPT-4o迷你版，性价比最高")
        private String description;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "提供商代码", example = "openai")
        private String providerCode;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "模型代码", example = "gpt-4o-mini")
        private String modelCode;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "上下文窗口大小", example = "128000")
        private Integer contextWindow;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持OCR识别", example = "true")
        private Boolean supportsOcr;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持视觉理解", example = "true")
        private Boolean supportsVision;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持深度思考", example = "true")
        private Boolean supportsThinking;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持工具调用", example = "true")
        private Boolean supportsToolCall;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持图像输入", example = "true")
        private Boolean supportsImageInput;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持视频输入", example = "false")
        private Boolean supportsVideoInput;
        
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否支持音频输入", example = "false")
        private Boolean supportsAudioInput;
    }

    @PostMapping("/{conversationId}/generate-title")
    @RequireAuth
    @Operation(summary = "生成对话标题", 
               description = "使用AI分析对话内容并生成简洁的标题（5-15字）。" +
                           "建议在用户发送第一条消息并收到AI回复后调用此接口。" +
                           "生成的标题会自动保存到对话记录中。")
    public Result<String> generateTitle(
            @Parameter(description = "对话ID", required = true)
            @PathVariable Long conversationId) {
        
        Long userId = SecurityUtils.getCurrentUserId().get();
        log.info("生成对话标题请求，conversationId: {}, userId: {}", conversationId, userId);
        
        try {
            String title = conversationApplicationService.generateConversationTitle(conversationId, userId);
            log.info("对话标题生成成功，conversationId: {}, title: {}", conversationId, title);
            return Result.success("标题生成成功", title);  // 明确指定message和data
        } catch (BizException e) {
            log.warn("对话标题生成失败，conversationId: {}, 错误: {}", conversationId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("对话标题生成异常，conversationId: " + conversationId, e);
            throw new BizException(ErrorCodeEnum.CONVERSATION_TITLE_GENERATION_FAILED, 
                "标题生成失败：" + e.getMessage());
        }
    }
}
