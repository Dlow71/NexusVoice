package com.nexusvoice.infrastructure.ai.service;

import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.service.ConversationTitleGenerator;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI驱动的对话标题生成器实现
 * 使用AI模型分析对话内容并生成简洁标题
 *
 * @author NexusVoice
 * @since 2025-01-26
 */
@Slf4j
@Service
public class AiConversationTitleGenerator implements ConversationTitleGenerator {

    private final DynamicAiModelBeanManager modelBeanManager;

    /**
     * 使用的AI模型（使用便宜的小模型节省成本）
     */
    private static final String DEFAULT_MODEL = "openai:gpt-oss-20b";

    /**
     * 标题生成提示词模板
     */
    private static final String TITLE_PROMPT_TEMPLATE = 
        "你是一个专业的对话标题生成助手。请根据以下对话内容，生成一个简洁、准确的标题。\n\n" +
        "要求：\n" +
        "1. 标题长度：5-15个字\n" +
        "2. 准确概括对话的核心主题\n" +
        "3. 使用简洁、口语化的表达\n" +
        "4. 不要使用标点符号\n" +
        "5. 只返回标题文字，不要其他内容\n\n" +
        "对话内容：\n%s\n\n" +
        "标题：";

    public AiConversationTitleGenerator(DynamicAiModelBeanManager modelBeanManager) {
        this.modelBeanManager = modelBeanManager;
    }

    @Override
    public String generateTitle(List<ConversationMessage> messages) {
        // 1. 验证消息
        if (!canGenerateTitle(messages)) {
            log.warn("对话消息不足，无法生成标题，消息数量：{}", messages.size());
            throw new BizException(ErrorCodeEnum.CONVERSATION_TITLE_TOO_SHORT);
        }

        try {
            // 2. 构建对话上下文（只取前2条消息）
            String conversationContext = buildConversationContext(messages);
            
            // 3. 构建AI请求
            ChatRequest chatRequest = buildChatRequest(conversationContext);
            
            // 4. 调用AI生成标题
            log.info("开始生成对话标题，使用模型：{}", DEFAULT_MODEL);
            AiChatService chatService = modelBeanManager.getServiceByModelKey(DEFAULT_MODEL);
            ChatResponse response = chatService.chat(chatRequest);
            
            // 5. 提取并清理标题
            String title = extractAndCleanTitle(response.getContent());
            
            log.info("对话标题生成成功：{}", title);
            return title;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI生成对话标题失败", e);
            throw new BizException(ErrorCodeEnum.CONVERSATION_TITLE_GENERATION_FAILED, 
                "标题生成失败：" + e.getMessage());
        }
    }

    /**
     * 构建对话上下文
     * 只取前2条消息（用户问题 + AI回复）
     */
    private String buildConversationContext(List<ConversationMessage> messages) {
        StringBuilder context = new StringBuilder();
        int count = 0;
        
        for (ConversationMessage msg : messages) {
            if (count >= 2) {
                break;
            }
            
            String role = msg.isFromUser() ? "用户" : "AI";
            String content = msg.getContent();
            
            // 限制内容长度，避免token浪费
            if (content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            
            context.append(role).append("：").append(content).append("\n");
            count++;
        }
        
        return context.toString();
    }

    /**
     * 构建AI聊天请求
     */
    private ChatRequest buildChatRequest(String conversationContext) {
        // 构建提示词
        String prompt = String.format(TITLE_PROMPT_TEMPLATE, conversationContext);
        
        // 创建用户消息
        List<ChatMessage> messageList = new ArrayList<>();
        messageList.add(ChatMessage.user(prompt));
        
        // 构建请求
        return ChatRequest.builder()
                .messages(messageList)
                .temperature(0.7)  // 适中的创造性
                .maxTokens(50)     // 标题很短，50个token足够
                .enableWebSearch(false)  // 不需要联网搜索
                .build();
    }

    /**
     * 提取并清理标题
     */
    private String extractAndCleanTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.trim().isEmpty()) {
            throw new BizException(ErrorCodeEnum.CONVERSATION_TITLE_GENERATION_FAILED, 
                "AI返回的标题为空");
        }
        
        // 清理标题
        String cleaned = rawTitle.trim()
                .replaceAll("[\"'`]", "")  // 移除引号
                .replaceAll("[\n\r]", "")  // 移除换行
                .replaceAll("^标题[:：]", "")  // 移除"标题："前缀
                .trim();
        
        // 长度限制
        if (cleaned.length() > 20) {
            cleaned = cleaned.substring(0, 20);
        }
        
        // 如果标题太短（少于2个字），使用默认值
        if (cleaned.length() < 2) {
            log.warn("生成的标题过短：{}，使用默认标题", cleaned);
            return "新对话";
        }
        
        return cleaned;
    }
}
