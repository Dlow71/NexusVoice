package com.nexusvoice.infrastructure.rag.markdown;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Flexmark解析器封装
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Slf4j
@Component
public class FlexmarkParser {
    
    private final Parser parser;
    
    /**
     * 构造函数，初始化Flexmark解析器
     */
    public FlexmarkParser() {
        MutableDataSet options = new MutableDataSet();
        
        // 启用扩展：表格、删除线
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create()
        ));
        
        this.parser = Parser.builder(options).build();
        log.info("FlexmarkParser初始化完成，已启用表格、删除线扩展");
    }
    
    /**
     * 解析Markdown为AST（抽象语法树）
     * 
     * @param markdown Markdown文本
     * @return AST根节点
     */
    public Node parse(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            log.warn("尝试解析空的Markdown文本");
            return null;
        }
        
        try {
            Node document = parser.parse(markdown);
            log.debug("Markdown解析成功，内容长度：{}", markdown.length());
            return document;
        } catch (Exception e) {
            log.error("Markdown解析失败", e);
            throw new RuntimeException("Markdown解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证Markdown语法
     * 
     * @param markdown Markdown文本
     * @return 是否有效
     */
    public boolean isValidMarkdown(String markdown) {
        try {
            Node node = parse(markdown);
            return node != null;
        } catch (Exception e) {
            log.warn("Markdown语法验证失败: {}", e.getMessage());
            return false;
        }
    }
}
