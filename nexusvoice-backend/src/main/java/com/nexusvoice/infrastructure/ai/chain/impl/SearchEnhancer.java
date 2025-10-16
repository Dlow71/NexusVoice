package com.nexusvoice.infrastructure.ai.chain.impl;

import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.infrastructure.ai.chain.AbstractChatEnhancer;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.tool.SimpleWebSearchTool;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索增强器
 * 负责执行联网搜索并将结果添加到请求中
 * 
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Component
@ConditionalOnBean(SimpleWebSearchTool.class)
public class SearchEnhancer extends AbstractChatEnhancer {
    
    @Autowired(required = false)
    private SimpleWebSearchTool searchTool;
    
    @Value("${nexusvoice.ai.enhancement.search.enabled:true}")
    private boolean searchEnabled;
    
    @Override
    protected EnhancementContext doEnhance(EnhancementContext context) {
        if (searchTool == null) {
            log.warn("搜索工具未配置，跳过搜索增强");
            return context;
        }
        
        try {
            ChatRequest request = context.getEnhancedRequest();
            
            // 提取用户查询
            String userQuery = extractLastUserMessage(request.getMessages());
            if (userQuery == null || userQuery.trim().isEmpty()) {
                log.debug("未找到用户查询，跳过搜索增强");
                return context;
            }
            
            // 执行搜索
            log.info("执行联网搜索，查询：{}", userQuery);
            String searchResults = searchTool.searchWeb(userQuery);
            
            if (searchResults != null && !searchResults.isEmpty()) {
                // 保存搜索结果到上下文
                context.setSearchResults(searchResults);
                
                // 创建新的消息列表，将搜索结果作为系统消息插入
                List<ChatMessage> enhancedMessages = new ArrayList<>();
                
                // 添加搜索结果作为系统消息
                String searchSystemMessage = "【搜索结果】以下是相关的实时信息，请基于这些信息回答用户问题：\n\n" 
                        + searchResults + "\n\n【重要】请确保回答基于上述搜索结果，保持准确性和时效性。";
                enhancedMessages.add(ChatMessage.system(searchSystemMessage));
                
                // 添加原始消息
                enhancedMessages.addAll(request.getMessages());
                
                // 创建增强后的请求
                ChatRequest enhancedRequest = ChatRequest.builder()
                        .messages(enhancedMessages)
                        .model(request.getModel())
                        .temperature(request.getTemperature())
                        .maxTokens(request.getMaxTokens())
                        .topP(request.getTopP())
                        .frequencyPenalty(request.getFrequencyPenalty())
                        .presencePenalty(request.getPresencePenalty())
                        .stop(request.getStop())
                        .stream(request.getStream())
                        .userId(request.getUserId())
                        .conversationId(request.getConversationId())
                        .enableWebSearch(request.getEnableWebSearch())
                        .enableRag(request.getEnableRag())
                        .enableMultiModal(request.getEnableMultiModal())
                        .knowledgeBaseIds(request.getKnowledgeBaseIds())
                        .build();
                
                context.setEnhancedRequest(enhancedRequest);
                log.info("搜索增强完成，结果长度：{}", searchResults.length());
            }
            
        } catch (Exception e) {
            log.error("搜索增强失败", e);
            // 增强失败不影响主流程，返回原始上下文
        }
        
        return context;
    }
    
    @Override
    public boolean shouldProcess(EnhancementContext context) {
        return searchEnabled && 
               Boolean.TRUE.equals(context.getEnableWebSearch()) &&
               searchTool != null;
    }
    
    @Override
    public String getName() {
        return "搜索增强器";
    }
    
    /**
     * 提取最后一条用户消息
     */
    private String extractLastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.getRole() == MessageRole.USER) {
                return msg.getContent();
            }
        }
        return null;
    }
}
