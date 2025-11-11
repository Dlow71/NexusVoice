package com.nexusvoice.domain.rag.model.vo;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档树值对象
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Getter
public class DocumentTree {
    
    private final List<MarkdownNode> nodes;
    private final SegmentSplitConfig config;
    
    /**
     * 构造函数
     */
    public DocumentTree(SegmentSplitConfig config) {
        this.nodes = new ArrayList<>();
        this.config = config;
    }
    
    /**
     * 添加节点
     */
    public void addNode(MarkdownNode node) {
        nodes.add(node);
    }
    
    /**
     * 层次化分割（对外接口）
     */
    public List<ProcessedSegment> splitHierarchically(SegmentSplitConfig splitConfig) {
        return performHierarchicalSplit();
    }
    
    /**
     * 执行层次化分割
     */
    public List<ProcessedSegment> performHierarchicalSplit() {
        List<ProcessedSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentLength = 0;
        String currentTitleContext = null;
        
        for (MarkdownNode node : nodes) {
            String content = node.getContent();
            int length = node.length();
            
            // 更新标题上下文
            if (node.isHeading()) {
                currentTitleContext = content;
            }
            
            // 一级标题强制分段
            if (node.isH1() && currentLength > 0) {
                segments.add(createSegment(current.toString(), segments.size(), currentTitleContext));
                current.setLength(0);
                currentLength = 0;
            }
            
            // 长度超限分段
            if (currentLength + length > config.getMaxLength() 
                && currentLength > config.getMinLength()) {
                segments.add(createSegment(current.toString(), segments.size(), currentTitleContext));
                current.setLength(0);
                currentLength = 0;
            }
            
            current.append(content).append("\n\n");
            currentLength += length;
        }
        
        // 最后一段
        if (currentLength > 0) {
            segments.add(createSegment(current.toString(), segments.size(), currentTitleContext));
        }
        
        return segments;
    }
    
    /**
     * 创建段落
     */
    private ProcessedSegment createSegment(String content, int order, String titleContext) {
        return ProcessedSegment.builder()
                .content(content.trim())
                .type(ProcessedSegment.SegmentType.TEXT)
                .order(order)
                .titleContext(titleContext)
                .build();
    }
    
    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }
}
