package com.nexusvoice.domain.conversation.model;

import com.nexusvoice.domain.common.BaseEntity;
import com.nexusvoice.domain.conversation.constant.ConversationStatus;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话聚合根
 * 管理用户与AI的对话会话
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {

    /**
     * 对话标题
     */
    private String title;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色ID（可为空）
     */
    private Long roleId;

    /**
     * AI模型名称
     */
    private String modelName;

    /**
     * 对话状态
     */
    private ConversationStatus status;

    /**
     * 对话消息列表
     */
    private List<ConversationMessage> messages;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 对话配置参数（JSON格式）
     * 包含temperature、maxTokens等模型参数
     */
    private String configParams;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    /**
     * 添加消息到对话
     */
    public void addMessage(ConversationMessage message) {
        if (this.messages == null) {
            this.messages = new java.util.ArrayList<>();
        }
        message.setConversationId(this.getId());
        this.messages.add(message);
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 更新对话标题
     */
    public void updateTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
            this.setUpdatedAt(LocalDateTime.now());
        }
    }

    /**
     * 激活对话
     */
    public void activate() {
        this.status = ConversationStatus.ACTIVE;
        this.lastActiveAt = LocalDateTime.now();
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 归档对话
     */
    public void archive() {
        this.status = ConversationStatus.ARCHIVED;
        this.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 检查对话是否属于指定用户
     */
    public boolean belongsToUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /**
     * 获取最后一条消息
     */
    public ConversationMessage getLastMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    /**
     * 验证模型一致性
     * 确保会话绑定的模型不被随意切换
     * 
     * @param requestModelName 请求中指定的模型名称
     * @throws BizException 如果模型不一致
     */
    public void validateModelConsistency(String requestModelName) {
        if (requestModelName == null || requestModelName.trim().isEmpty()) {
            // 如果请求没有指定模型，使用会话默认模型，不需要验证
            return;
        }
        
        // 统一模型名称格式（添加provider前缀）
        String normalizedRequestModel = normalizeModelName(requestModelName);
        String normalizedSessionModel = normalizeModelName(this.modelName);
        
        // 检查模型是否一致
        if (!normalizedRequestModel.equals(normalizedSessionModel)) {
            log.warn("会话{}尝试切换模型：{} -> {}，拒绝切换", 
                this.getId(), normalizedSessionModel, normalizedRequestModel);
            
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                String.format("当前会话已绑定模型[%s]，无法切换为[%s]。如需使用不同模型，请创建新会话", 
                    normalizedSessionModel, normalizedRequestModel));
        }
    }
    
    /**
     * 标准化模型名称
     * 如果没有provider前缀，默认添加openai:
     * 
     * @param modelName 原始模型名称
     * @return 标准化后的模型名称
     */
    private String normalizeModelName(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return "openai:gpt-4o-mini"; // 默认模型
        }
        
        String trimmed = modelName.trim();
        if (!trimmed.contains(":")) {
            // 没有provider前缀，默认为openai
            return "openai:" + trimmed;
        }
        
        return trimmed;
    }
}
