package com.nexusvoice.infrastructure.rag.markdown;

import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.MarkdownNode;
import com.nexusvoice.domain.rag.model.vo.SegmentSplitConfig;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.util.ast.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档树构建器
 * 将Flexmark AST转换为Domain层的DocumentTree
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Slf4j
@Component
public class DocumentTreeBuilder {
    
    /**
     * 构建文档树（原文模式，保留Markdown原始格式）
     * 
     * @param document Flexmark AST根节点
     * @return 文档树
     */
    public DocumentTree buildOriginalTree(Node document) {
        return buildRawDocumentTree(document, SegmentSplitConfig.defaultConfig());
    }
    
    /**
     * 构建文档树（原文模式，保留Markdown原始格式）
     * 
     * @param document Flexmark AST根节点
     * @param config 分段配置
     * @return 文档树
     */
    public DocumentTree buildRawDocumentTree(Node document, SegmentSplitConfig config) {
        if (document == null) {
            log.warn("尝试构建空的AST文档树");
            return new DocumentTree(config);
        }
        
        DocumentTree tree = new DocumentTree(config);
        int nodeCount = 0;
        
        // 遍历AST树的所有子节点
        for (Node child : document.getChildren()) {
            MarkdownNode node = processNode(child);
            if (node != null) {
                tree.addNode(node);
                nodeCount++;
            }
        }
        
        log.info("文档树构建完成，共{}个节点", nodeCount);
        return tree;
    }
    
    /**
     * 处理单个AST节点
     */
    private MarkdownNode processNode(Node node) {
        if (node instanceof Heading) {
            return createHeadingNode((Heading) node);
        } else if (node instanceof Paragraph) {
            return createParagraphNode((Paragraph) node);
        } else if (node instanceof FencedCodeBlock) {
            return createCodeBlockNode((FencedCodeBlock) node);
        } else if (node instanceof IndentedCodeBlock) {
            return createIndentedCodeBlockNode((IndentedCodeBlock) node);
        } else if (node instanceof TableBlock) {
            return createTableNode((TableBlock) node);
        } else if (node instanceof Image) {
            return createImageNode((Image) node);
        } else if (node instanceof BulletList) {
            return createListNode(node, "ul");
        } else if (node instanceof OrderedList) {
            return createListNode(node, "ol");
        } else if (node instanceof BlockQuote) {
            return createBlockQuoteNode((BlockQuote) node);
        }
        
        // 其他类型的节点暂不处理
        return null;
    }
    
    /**
     * 创建标题节点
     */
    private MarkdownNode createHeadingNode(Heading heading) {
        int level = heading.getLevel();
        String text = heading.getText().toString();
        String content = "#".repeat(level) + " " + text;
        
        return new MarkdownNode(
            MarkdownNode.NodeType.HEADING,
            level,
            content,
            null,
            null
        );
    }
    
    /**
     * 创建段落节点
     */
    private MarkdownNode createParagraphNode(Paragraph paragraph) {
        String content = paragraph.getContentChars().toString();
        
        return new MarkdownNode(
            MarkdownNode.NodeType.PARAGRAPH,
            0,
            content,
            null,
            null
        );
    }
    
    /**
     * 创建代码块节点（围栏式）
     */
    private MarkdownNode createCodeBlockNode(FencedCodeBlock codeBlock) {
        String code = codeBlock.getContentChars().toString();
        String lang = codeBlock.getInfo().toString();
        String content = "```" + lang + "\n" + code + "```";
        
        return new MarkdownNode(
            MarkdownNode.NodeType.CODE_BLOCK,
            0,
            content,
            lang,
            null
        );
    }
    
    /**
     * 创建代码块节点（缩进式）
     */
    private MarkdownNode createIndentedCodeBlockNode(IndentedCodeBlock codeBlock) {
        String code = codeBlock.getContentChars().toString();
        String content = "```\n" + code + "```";
        
        return new MarkdownNode(
            MarkdownNode.NodeType.CODE_BLOCK,
            0,
            content,
            "",
            null
        );
    }
    
    /**
     * 创建表格节点
     */
    private MarkdownNode createTableNode(TableBlock table) {
        String content = table.getChars().toString();
        
        return new MarkdownNode(
            MarkdownNode.NodeType.TABLE,
            0,
            content,
            null,
            null
        );
    }
    
    /**
     * 创建图片节点
     */
    private MarkdownNode createImageNode(Image image) {
        String url = image.getUrl().toString();
        String alt = image.getText().toString();
        String content = "![" + alt + "](" + url + ")";
        
        Map<String, String> attributes = new HashMap<>();
        attributes.put("url", url);
        attributes.put("alt", alt);
        
        return new MarkdownNode(
            MarkdownNode.NodeType.IMAGE,
            0,
            content,
            null,
            attributes
        );
    }
    
    /**
     * 创建列表节点
     */
    private MarkdownNode createListNode(Node list, String listType) {
        String content = list.getChars().toString();
        
        Map<String, String> attributes = new HashMap<>();
        attributes.put("type", listType);
        
        return new MarkdownNode(
            MarkdownNode.NodeType.LIST,
            0,
            content,
            null,
            attributes
        );
    }
    
    /**
     * 创建引用块节点
     */
    private MarkdownNode createBlockQuoteNode(BlockQuote blockQuote) {
        String content = blockQuote.getChars().toString();
        
        return new MarkdownNode(
            MarkdownNode.NodeType.BLOCKQUOTE,
            0,
            content,
            null,
            null
        );
    }
}
