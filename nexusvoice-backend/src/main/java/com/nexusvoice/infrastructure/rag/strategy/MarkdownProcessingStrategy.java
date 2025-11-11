package com.nexusvoice.infrastructure.rag.strategy;

import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.MarkdownNode;
import com.nexusvoice.domain.rag.model.vo.ProcessedSegment;
import com.nexusvoice.domain.rag.model.vo.SegmentSplitConfig;
import com.nexusvoice.domain.rag.service.DocumentProcessingStrategy;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.rag.markdown.DocumentTreeBuilder;
import com.nexusvoice.infrastructure.rag.markdown.FlexmarkParser;
import com.nexusvoice.infrastructure.rag.service.MarkdownTranslationService;
import com.vladsch.flexmark.util.ast.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Markdown文档处理策略
 * 实现Markdown文档的两阶段处理：
 * 1. 结构化解析与原文分割
 * 2. 翻译增强与智能分割
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownProcessingStrategy implements DocumentProcessingStrategy {
    
    private final FlexmarkParser flexmarkParser;
    private final DocumentTreeBuilder documentTreeBuilder;
    private final MarkdownTranslationService translationService;
    
    @Override
    public boolean supports(FileDetail fileDetail) {
        if (fileDetail == null) {
            return false;
        }
        return fileDetail.getFileType() == FileType.MARKDOWN;
    }
    
    @Override
    public List<ProcessedSegment> parseAndSplit(FileDetail fileDetail, byte[] fileContent, SegmentSplitConfig splitConfig) {
        log.info("开始Markdown文档结构化解析与原文分割 - 文件ID: {}, 文件名: {}", 
                fileDetail.getId(), fileDetail.getOriginalName());
        
        try {
            // 1. 验证参数
            if (fileContent == null || fileContent.length == 0) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件内容为空");
            }
            splitConfig.validate();
            
            // 2. 转换为字符串
            String markdownContent = new String(fileContent, StandardCharsets.UTF_8);
            log.debug("Markdown内容长度: {} 字符", markdownContent.length());
            
            // 3. 解析为Flexmark AST
            Node document = flexmarkParser.parse(markdownContent);
            log.debug("Flexmark AST解析完成");
            
            // 4. 构建Domain层DocumentTree（原文模式）
            DocumentTree documentTree = documentTreeBuilder.buildOriginalTree(document);
            log.info("文档树构建完成 - 节点数: {}", documentTree.getNodes().size());
            
            // 5. 层次化分割
            List<ProcessedSegment> segments = documentTree.splitHierarchically(splitConfig);
            log.info("阶段1完成 - 原文分割段落数: {}", segments.size());
            
            return segments;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Markdown文档解析失败 - 文件ID: {}", fileDetail.getId(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "Markdown文档解析失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<ProcessedSegment> translateAndSmartSplit(DocumentTree documentTree, SegmentSplitConfig splitConfig) {
        log.info("开始Markdown文档翻译增强与智能分割 - 节点数: {}", documentTree.getNodes().size());
        
        try {
            // 1. 验证参数
            if (documentTree == null || documentTree.getNodes().isEmpty()) {
                throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文档树为空");
            }
            splitConfig.validate();
            
            // 2. 翻译特殊节点（代码块、表格、图片等）
            DocumentTree translatedTree = translateSpecialNodes(documentTree);
            log.info("特殊节点翻译完成");
            
            // 3. 智能二次分割
            List<ProcessedSegment> segments = smartSplit(translatedTree, splitConfig);
            log.info("阶段2完成 - 智能分割段落数: {}", segments.size());
            
            return segments;
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Markdown文档翻译与智能分割失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "文档翻译与智能分割失败: " + e.getMessage());
        }
    }
    
    /**
     * 翻译特殊节点
     * 
     * @param documentTree 原始文档树
     * @return 翻译后的文档树
     */
    private DocumentTree translateSpecialNodes(DocumentTree documentTree) {
        try {
            return translationService.translateSpecialNodes(documentTree);
        } catch (Exception e) {
            log.error("翻译特殊节点失败，继续使用原文档树", e);
            return documentTree;
        }
    }
    
    /**
     * 智能二次分割
     * 三层策略：段落分割 -> 句子分割 -> 强制截断
     * 
     * @param documentTree 文档树
     * @param splitConfig 分段配置
     * @return 分割后的段落列表
     */
    private List<ProcessedSegment> smartSplit(DocumentTree documentTree, SegmentSplitConfig splitConfig) {
        List<ProcessedSegment> result = new ArrayList<>();
        
        for (MarkdownNode node : documentTree.getNodes()) {
            // 获取节点内容（优先使用翻译内容）
            String content = node.getTranslatedContent() != null 
                    ? node.getTranslatedContent() 
                    : node.getContent();
            
            if (content == null || content.isEmpty()) {
                continue;
            }
            
            // 1. 尝试按段落分割（\n\n）
            List<String> paragraphs = splitByParagraph(content);
            
            for (String paragraph : paragraphs) {
                if (paragraph.length() <= splitConfig.getMaxLength()) {
                    // 段落长度合适，直接添加
                    result.add(createSegment(paragraph, node));
                } else {
                    // 2. 段落过长，尝试按句子分割
                    List<String> sentences = splitBySentence(paragraph);
                    
                    StringBuilder buffer = new StringBuilder();
                    for (String sentence : sentences) {
                        if (buffer.length() + sentence.length() <= splitConfig.getMaxLength()) {
                            buffer.append(sentence);
                        } else {
                            if (buffer.length() > 0) {
                                result.add(createSegment(buffer.toString(), node));
                                buffer = new StringBuilder();
                            }
                            
                            // 3. 单个句子仍然过长，强制截断
                            if (sentence.length() > splitConfig.getMaxLength()) {
                                result.addAll(forceSplit(sentence, splitConfig.getMaxLength(), node));
                            } else {
                                buffer.append(sentence);
                            }
                        }
                    }
                    
                    if (buffer.length() > 0) {
                        result.add(createSegment(buffer.toString(), node));
                    }
                }
            }
        }
        
        log.debug("智能分割完成 - 输入节点数: {}, 输出段落数: {}", 
                documentTree.getNodes().size(), result.size());
        
        return result;
    }
    
    /**
     * 按段落分割（双换行符）
     */
    private List<String> splitByParagraph(String content) {
        return Arrays.stream(content.split("\n\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
    
    /**
     * 按句子分割（中英文句号）
     */
    private List<String> splitBySentence(String content) {
        List<String> sentences = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            buffer.append(c);
            
            // 判断是否是句子结束符
            if (c == '。' || c == '.' || c == '!' || c == '?' || c == '！' || c == '？') {
                sentences.add(buffer.toString().trim());
                buffer = new StringBuilder();
            }
        }
        
        if (buffer.length() > 0) {
            sentences.add(buffer.toString().trim());
        }
        
        return sentences.stream().filter(s -> !s.isEmpty()).toList();
    }
    
    /**
     * 强制截断（保证不超过最大长度）
     */
    private List<ProcessedSegment> forceSplit(String content, int maxLength, MarkdownNode node) {
        List<ProcessedSegment> result = new ArrayList<>();
        
        for (int i = 0; i < content.length(); i += maxLength) {
            int end = Math.min(i + maxLength, content.length());
            String chunk = content.substring(i, end);
            result.add(createSegment(chunk, node));
        }
        
        return result;
    }
    
    /**
     * 创建ProcessedSegment
     */
    private ProcessedSegment createSegment(String content, MarkdownNode node) {
        return ProcessedSegment.builder()
                .content(content)
                .nodeType(node.getType())
                .headingLevel(node.getLevel())
                .language(node.getLanguage())
                .metadata(node.getMetadata())
                .build();
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return Arrays.asList("md", "markdown");
    }
    
    @Override
    public String getStrategyName() {
        return "Markdown处理策略";
    }
}
