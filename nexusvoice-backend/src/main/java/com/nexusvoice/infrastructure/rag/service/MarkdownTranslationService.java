package com.nexusvoice.infrastructure.rag.service;

import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.MarkdownNode;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown翻译服务
 * 负责将特殊节点（代码块、表格、图片）翻译为易于理解的中文描述
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownTranslationService {
    
    private final DynamicAiModelBeanManager aiModelBeanManager;
    
    // 默认使用的翻译模型（可从配置读取）
    private static final String DEFAULT_TRANSLATION_MODEL = "openai:gpt-4o-mini";
    
    /**
     * 翻译文档树中的特殊节点
     * 
     * @param documentTree 文档树
     * @return 翻译后的文档树
     */
    public DocumentTree translateSpecialNodes(DocumentTree documentTree) {
        if (documentTree == null || documentTree.getNodes().isEmpty()) {
            log.warn("文档树为空，跳过翻译");
            return documentTree;
        }
        
        log.info("开始翻译特殊节点 - 节点总数: {}", documentTree.getNodes().size());
        
        int translatedCount = 0;
        for (MarkdownNode node : documentTree.getNodes()) {
            if (node.needsTranslation()) {
                try {
                    String translatedContent = translateNode(node);
                    node.setTranslatedContent(translatedContent);
                    translatedCount++;
                    
                    log.debug("节点翻译完成 - 类型: {}, 原始长度: {}, 翻译长度: {}", 
                            node.getType(), node.length(), translatedContent.length());
                } catch (Exception e) {
                    log.error("节点翻译失败 - 类型: {}, 错误: {}", node.getType(), e.getMessage(), e);
                    // 翻译失败不中断流程，继续处理其他节点
                }
            }
        }
        
        log.info("特殊节点翻译完成 - 翻译数量: {} / {}", translatedCount, documentTree.getNodes().size());
        
        return documentTree;
    }
    
    /**
     * 翻译单个节点
     * 
     * @param node Markdown节点
     * @return 翻译后的内容
     */
    private String translateNode(MarkdownNode node) {
        String prompt = buildTranslationPrompt(node);
        String response = callAiModel(prompt);
        return parseTranslationResponse(response, node);
    }
    
    /**
     * 构建翻译Prompt
     * 
     * @param node Markdown节点
     * @return Prompt文本
     */
    private String buildTranslationPrompt(MarkdownNode node) {
        StringBuilder prompt = new StringBuilder();
        
        switch (node.getType()) {
            case CODE_BLOCK:
                prompt.append("请为以下代码块生成简洁的中文描述和注释说明：\n\n");
                prompt.append("编程语言：").append(node.getLanguage() != null ? node.getLanguage() : "未知").append("\n\n");
                prompt.append("```").append(node.getLanguage()).append("\n");
                prompt.append(node.getContent()).append("\n");
                prompt.append("```\n\n");
                prompt.append("要求：\n");
                prompt.append("1. 说明代码的主要功能和目的\n");
                prompt.append("2. 解释关键逻辑和算法\n");
                prompt.append("3. 指出重要的参数和返回值\n");
                prompt.append("4. 使用简洁的技术语言\n");
                prompt.append("5. 不要重复代码本身，只提供理解性描述\n");
                break;
                
            case TABLE:
                prompt.append("请为以下Markdown表格生成简洁的中文描述：\n\n");
                prompt.append(node.getContent()).append("\n\n");
                prompt.append("要求：\n");
                prompt.append("1. 总结表格的主要内容和结构\n");
                prompt.append("2. 说明表格包含的数据类型\n");
                prompt.append("3. 突出关键信息和数据规律\n");
                prompt.append("4. 使用简洁明了的语言\n");
                break;
                
            case IMAGE:
                prompt.append("请为以下图片生成中文描述：\n\n");
                prompt.append("图片信息：").append(node.getContent()).append("\n\n");
                if (node.getAttributes() != null) {
                    prompt.append("图片URL：").append(node.getAttributes().get("url")).append("\n");
                    prompt.append("图片说明：").append(node.getAttributes().get("alt")).append("\n\n");
                }
                prompt.append("要求：\n");
                prompt.append("1. 根据图片的URL和alt文本推测图片内容\n");
                prompt.append("2. 说明图片在文档中的作用\n");
                prompt.append("3. 使用简洁的描述性语言\n");
                break;
                
            default:
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, 
                        "不支持的节点类型翻译: " + node.getType());
        }
        
        return prompt.toString();
    }
    
    /**
     * 调用AI模型
     * 
     * @param prompt Prompt文本
     * @return AI响应
     */
    private String callAiModel(String prompt) {
        try {
            // 获取AI服务
            AiChatService aiService = aiModelBeanManager.getServiceByModelKey(DEFAULT_TRANSLATION_MODEL);
            
            if (aiService == null) {
                throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                        "未找到翻译模型服务: " + DEFAULT_TRANSLATION_MODEL);
            }
            
            // 构建请求
            ChatRequest request = ChatRequest.builder()
                    .messages(buildMessages(prompt))
                    .temperature(0.3) // 较低温度，保证翻译准确性
                    .maxTokens(2000)
                    .build();
            
            // 调用模型
            ChatResponse response = aiService.chat(request);
            
            if (response == null || response.getContent() == null) {
                throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "AI模型返回空响应");
            }
            
            return response.getContent();
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用AI模型失败: {}", e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "调用AI模型失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建消息列表
     * 
     * @param userMessage 用户消息
     * @return 消息列表
     */
    private List<ChatMessage> buildMessages(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 系统消息
        messages.add(ChatMessage.system(
                "你是一个专业的技术文档翻译助手，擅长将代码、表格等技术内容转换为易于理解的中文描述。"));
        
        // 用户消息
        messages.add(ChatMessage.user(userMessage));
        
        return messages;
    }
    
    /**
     * 解析翻译响应
     * 
     * @param response AI响应
     * @param node 原始节点
     * @return 解析后的翻译内容
     */
    private String parseTranslationResponse(String response, MarkdownNode node) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("AI返回空响应，使用原始内容");
            return node.getContent();
        }
        
        // 清理响应（移除markdown代码块标记等）
        String cleaned = response.trim();
        
        // 如果响应被包裹在代码块中，提取内容
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastBackticks = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastBackticks > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        
        return cleaned;
    }
    
    /**
     * 批量翻译节点（提高效率）
     * 
     * @param nodes 节点列表
     * @return 翻译后的节点列表
     */
    public List<MarkdownNode> batchTranslateNodes(List<MarkdownNode> nodes) {
        log.info("开始批量翻译节点 - 数量: {}", nodes.size());
        
        List<MarkdownNode> needsTranslation = nodes.stream()
                .filter(MarkdownNode::needsTranslation)
                .toList();
        
        log.info("需要翻译的节点数: {}", needsTranslation.size());
        
        // TODO: 实现批量翻译逻辑，将多个节点合并到一个请求中
        // 当前实现：逐个翻译
        for (MarkdownNode node : needsTranslation) {
            try {
                String translatedContent = translateNode(node);
                node.setTranslatedContent(translatedContent);
            } catch (Exception e) {
                log.error("批量翻译节点失败 - 类型: {}", node.getType(), e);
            }
        }
        
        return nodes;
    }
}
