package com.nexusvoice.infrastructure.ai.strategy.impl;

import com.nexusvoice.domain.ai.model.AiModelInfo;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.strategy.ChatStrategy;
import com.nexusvoice.infrastructure.ai.tool.SimpleWebSearchTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索增强聊天策略
 * 在调用AI模型前，先进行联网搜索，将搜索结果作为上下文提供给模型
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
@ConditionalOnBean(SimpleWebSearchTool.class)
public class SearchEnhancedChatStrategy implements ChatStrategy {
    
    @Autowired
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired(required = false)
    private SimpleWebSearchTool searchTool;
    
    @Override
    public ChatResponse execute(ChatRequest request, AiModelInfo modelInfo, EnhancementContext context) {
        try {
            log.info("执行搜索增强聊天策略，模型：{}", modelInfo.getModelIdentifier());
            
            // 获取用户最后的问题
            String userQuery = extractLastUserMessage(request.getMessages());
            
            // 执行搜索（如果需要）
            String searchResults = null;
            if (shouldPerformSearch(userQuery) && searchTool != null) {
                log.info("执行联网搜索，查询：{}", userQuery);
                searchResults = searchTool.searchWeb(userQuery);
                context.setSearchResults(searchResults);
                log.info("搜索完成，结果长度：{}", searchResults != null ? searchResults.length() : 0);
            }
            
            // 构建增强的消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = buildEnhancedMessages(
                    request.getMessages(), searchResults);
            
            // 调用模型
            Response<AiMessage> response = chatLanguageModel.generate(messages);
            
            // 构建响应
            String content = response.content().text();
            
            // 估算令牌数
            ChatResponse.TokenUsage usage = ChatResponse.TokenUsage.builder()
                    .promptTokens(estimateTokenCount(buildPromptText(request.getMessages(), searchResults)))
                    .completionTokens(estimateTokenCount(content))
                    .totalTokens(estimateTokenCount(buildPromptText(request.getMessages(), searchResults) + content))
                    .build();
            
            ChatResponse chatResponse = ChatResponse.success(content, modelInfo.getModelIdentifier(), usage, null);
            
            // 添加搜索结果到元数据
            if (searchResults != null) {
                chatResponse.setMetadata("searchPerformed", true);
                chatResponse.setMetadata("searchResultsLength", searchResults.length());
            }
            
            return chatResponse;
            
        } catch (Exception e) {
            log.error("搜索增强聊天策略执行失败", e);
            return ChatResponse.error("聊天处理失败：" + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(ChatRequest request, AiModelInfo modelInfo) {
        // 当启用了联网搜索且搜索工具可用时支持
        return Boolean.TRUE.equals(request.getEnableWebSearch()) && searchTool != null;
    }
    
    @Override
    public String getName() {
        return "搜索增强聊天策略";
    }
    
    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }
    
    /**
     * 判断是否需要执行搜索
     */
    private boolean shouldPerformSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        String lower = query.toLowerCase();
        
        // 时效性关键词检测
        String[] timeKeywords = {
                "最新", "今天", "现在", "刚刚", "最近", "目前", "当前",
                "today", "now", "latest", "current", "recent"
        };
        
        for (String keyword : timeKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        
        // 事实性关键词检测
        String[] factKeywords = {
                "价格", "股票", "天气", "新闻", "比分", "汇率", "数据",
                "price", "stock", "weather", "news", "score", "rate", "data"
        };
        
        for (String keyword : factKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        
        // 年份检测（2020-2029）
        if (lower.matches(".*\\b20[2-9]\\d\\b.*")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.getRole() == com.nexusvoice.domain.conversation.constant.MessageRole.USER) {
                return msg.getContent();
            }
        }
        return null;
    }
    
    /**
     * 构建增强的消息列表
     */
    private List<dev.langchain4j.data.message.ChatMessage> buildEnhancedMessages(
            List<ChatMessage> messages, String searchResults) {
        
        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = new ArrayList<>();
        
        // 如果有搜索结果，添加为系统消息
        if (searchResults != null && !searchResults.isEmpty()) {
            String searchContext = "以下是相关的搜索结果，请基于这些信息回答用户问题：\n\n" + searchResults;
            langchainMessages.add(SystemMessage.from(searchContext));
        }
        
        // 转换原始消息
        for (ChatMessage message : messages) {
            switch (message.getRole()) {
                case SYSTEM:
                    langchainMessages.add(SystemMessage.from(message.getContent()));
                    break;
                case USER:
                    langchainMessages.add(UserMessage.from(message.getContent()));
                    break;
                case ASSISTANT:
                    langchainMessages.add(AiMessage.from(message.getContent()));
                    break;
                default:
                    log.warn("忽略不支持的消息角色：{}", message.getRole());
                    break;
            }
        }
        
        return langchainMessages;
    }
    
    /**
     * 构建提示文本
     */
    private String buildPromptText(List<ChatMessage> messages, String searchResults) {
        StringBuilder sb = new StringBuilder();
        if (searchResults != null) {
            sb.append(searchResults).append("\n");
        }
        for (ChatMessage message : messages) {
            sb.append(message.getContent()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 估算令牌数
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 3.0);
    }
}
