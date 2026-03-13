package com.nexusvoice.infrastructure.ai.converter;

import com.nexusvoice.domain.ai.model.AiChatRequest;
import com.nexusvoice.domain.ai.model.AiChatResponse;
import com.nexusvoice.domain.ai.model.AiMessage;
import com.nexusvoice.domain.ai.model.EnhancementContext;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI模型转换器
 * 负责在领域模型和基础设施模型之间进行转换
 * 实现防腐层（Anti-Corruption Layer）模式
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
public class AiModelConverter {

    /**
     * 将领域请求转换为基础设施请求
     */
    public static ChatRequest toInfrastructureRequest(AiChatRequest domainRequest) {
        if (domainRequest == null) {
            return null;
        }

        ChatRequest infraRequest = ChatRequest.builder()
                .model(domainRequest.getModel())
                .temperature(domainRequest.getTemperature())
                .maxTokens(domainRequest.getMaxTokens())
                .topP(domainRequest.getTopP())
                .frequencyPenalty(domainRequest.getFrequencyPenalty())
                .presencePenalty(domainRequest.getPresencePenalty())
                .stop(domainRequest.getStop())
                .stream(domainRequest.getStream())
                .userId(domainRequest.getUserId())
                .conversationId(domainRequest.getConversationId())
                .enableWebSearch(domainRequest.getEnableWebSearch())
                .enableRag(domainRequest.getEnableRag())
                .enableMultiModal(domainRequest.getEnableMultiModal())
                .knowledgeBaseIds(domainRequest.getKnowledgeBaseIds())
                .ragGroundingMode(domainRequest.getRagGroundingMode())
                // 添加图像字段转换
                .imageUrls(domainRequest.getImageUrls())
                .imageBase64(domainRequest.getImageBase64())
                .build();

        // 转换消息列表
        if (domainRequest.getMessages() != null) {
            List<ChatMessage> infraMessages = domainRequest.getMessages().stream()
                    .map(AiModelConverter::toInfrastructureMessage)
                    .collect(Collectors.toList());
            infraRequest.setMessages(infraMessages);
        }

        return infraRequest;
    }

    /**
     * 将基础设施请求转换为领域请求
     */
    public static AiChatRequest toDomainRequest(ChatRequest infraRequest) {
        if (infraRequest == null) {
            return null;
        }

        AiChatRequest domainRequest = AiChatRequest.builder()
                .model(infraRequest.getModel())
                .temperature(infraRequest.getTemperature())
                .maxTokens(infraRequest.getMaxTokens())
                .topP(infraRequest.getTopP())
                .frequencyPenalty(infraRequest.getFrequencyPenalty())
                .presencePenalty(infraRequest.getPresencePenalty())
                .stop(infraRequest.getStop())
                .stream(infraRequest.getStream())
                .userId(infraRequest.getUserId())
                .conversationId(infraRequest.getConversationId())
                .enableWebSearch(infraRequest.getEnableWebSearch())
                .enableRag(infraRequest.getEnableRag())
                .enableMultiModal(infraRequest.getEnableMultiModal())
                .knowledgeBaseIds(infraRequest.getKnowledgeBaseIds())
                .ragGroundingMode(infraRequest.getRagGroundingMode())
                // 添加图像字段转换
                .imageUrls(infraRequest.getImageUrls())
                .imageBase64(infraRequest.getImageBase64())
                .build();

        // 转换消息列表
        if (infraRequest.getMessages() != null) {
            List<AiMessage> domainMessages = infraRequest.getMessages().stream()
                    .map(AiModelConverter::toDomainMessage)
                    .collect(Collectors.toList());
            domainRequest.setMessages(domainMessages);
        }

        return domainRequest;
    }

    /**
     * 将领域消息转换为基础设施消息
     */
    public static ChatMessage toInfrastructureMessage(AiMessage domainMessage) {
        if (domainMessage == null) {
            return null;
        }

        // 将领域消息角色转换为基础设施消息角色
        com.nexusvoice.domain.conversation.constant.MessageRole infraRole;
        switch (domainMessage.getRole()) {
            case SYSTEM:
                infraRole = com.nexusvoice.domain.conversation.constant.MessageRole.SYSTEM;
                break;
            case USER:
                infraRole = com.nexusvoice.domain.conversation.constant.MessageRole.USER;
                break;
            case ASSISTANT:
                infraRole = com.nexusvoice.domain.conversation.constant.MessageRole.ASSISTANT;
                break;
            case FUNCTION:
                // 基础设施层不支持FUNCTION角色，转为ASSISTANT
                infraRole = com.nexusvoice.domain.conversation.constant.MessageRole.ASSISTANT;
                break;
            default:
                throw new IllegalArgumentException("Unknown message role: " + domainMessage.getRole());
        }
        
        return new ChatMessage(infraRole, domainMessage.getContent());
    }

    /**
     * 将基础设施消息转换为领域消息
     */
    public static AiMessage toDomainMessage(ChatMessage infraMessage) {
        if (infraMessage == null) {
            return null;
        }

        com.nexusvoice.domain.conversation.constant.MessageRole infraRole = infraMessage.getRole();
        String content = infraMessage.getContent();

        // 将基础设施消息角色转换为领域消息角色
        switch (infraRole) {
            case SYSTEM:
                return AiMessage.system(content);
            case USER:
                return AiMessage.user(content);
            case ASSISTANT:
                return AiMessage.assistant(content);
            default:
                // 其他角色统一转为ASSISTANT
                return AiMessage.assistant(content);
        }
    }

    /**
     * 将基础设施响应转换为领域响应
     */
    public static AiChatResponse toDomainResponse(ChatResponse infraResponse) {
        if (infraResponse == null) {
            return null;
        }

        AiChatResponse domainResponse = new AiChatResponse();
        domainResponse.setContent(infraResponse.getContent());
        domainResponse.setModel(infraResponse.getModel());
        domainResponse.setSuccess(infraResponse.getSuccess());
        domainResponse.setErrorMessage(infraResponse.getErrorMessage());
        domainResponse.setResponseTimeMs(infraResponse.getResponseTimeMs());

        // 转换Token使用情况
        if (infraResponse.getUsage() != null) {
            AiChatResponse.TokenUsage tokenUsage = new AiChatResponse.TokenUsage(
                    infraResponse.getUsage().getPromptTokens(),
                    infraResponse.getUsage().getCompletionTokens()
            );
            domainResponse.setUsage(tokenUsage);
        }

        return domainResponse;
    }

    /**
     * 将领域响应转换为基础设施响应
     */
    public static ChatResponse toInfrastructureResponse(AiChatResponse domainResponse) {
        if (domainResponse == null) {
            return null;
        }

        ChatResponse infraResponse = new ChatResponse();
        infraResponse.setContent(domainResponse.getContent());
        infraResponse.setModel(domainResponse.getModel());
        infraResponse.setSuccess(domainResponse.getSuccess());
        infraResponse.setErrorMessage(domainResponse.getErrorMessage());
        infraResponse.setResponseTimeMs(domainResponse.getResponseTimeMs());

        // 转换Token使用情况
        if (domainResponse.getUsage() != null) {
            ChatResponse.TokenUsage tokenUsage = new ChatResponse.TokenUsage();
            tokenUsage.setPromptTokens(domainResponse.getUsage().getPromptTokens());
            tokenUsage.setCompletionTokens(domainResponse.getUsage().getCompletionTokens());
            tokenUsage.setTotalTokens(domainResponse.getUsage().getTotalTokens());
            infraResponse.setUsage(tokenUsage);
        }

        return infraResponse;
    }

    /**
     * 将基础设施增强上下文转换为领域增强上下文
     */
    public static EnhancementContext toDomainContext(ChatRequest originalRequest, 
                                                     ChatRequest enhancedRequest,
                                                     Boolean enableWebSearch,
                                                     Boolean enableRag,
                                                     Boolean enableMultiModal) {
        return EnhancementContext.builder()
                .originalRequest(toDomainRequest(originalRequest))
                .enhancedRequest(toDomainRequest(enhancedRequest))
                .enableWebSearch(enableWebSearch)
                .enableRag(enableRag)
                .enableMultiModal(enableMultiModal)
                .build();
    }

    /**
     * 从领域增强上下文获取基础设施请求
     */
    public static ChatRequest getInfrastructureRequest(EnhancementContext domainContext) {
        if (domainContext == null) {
            return null;
        }
        
        if (domainContext.getEnhancedRequest() != null) {
            return toInfrastructureRequest(domainContext.getEnhancedRequest());
        } else if (domainContext.getOriginalRequest() != null) {
            return toInfrastructureRequest(domainContext.getOriginalRequest());
        }
        
        return null;
    }

    /**
     * 更新领域增强上下文的请求
     */
    public static void updateContextRequest(EnhancementContext domainContext, ChatRequest infraRequest) {
        if (domainContext != null && infraRequest != null) {
            domainContext.setEnhancedRequest(toDomainRequest(infraRequest));
        }
    }
}
