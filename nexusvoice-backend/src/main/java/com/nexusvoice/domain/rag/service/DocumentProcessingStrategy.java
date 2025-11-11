package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.ProcessedSegment;
import com.nexusvoice.domain.rag.model.vo.SegmentSplitConfig;

import java.util.List;

/**
 * 文档处理策略接口
 * 定义了文档解析、分割、向量化的标准流程
 * 不同文档类型（Markdown、PDF、Word等）实现不同的处理策略
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
public interface DocumentProcessingStrategy {
    
    /**
     * 判断是否支持该文件类型
     * 
     * @param fileDetail 文件详情
     * @return 是否支持
     */
    boolean supports(FileDetail fileDetail);
    
    /**
     * 阶段1：结构化解析与原文分割
     * 将文档解析为结构化的文档树，并进行层次化分割
     * 
     * @param fileDetail 文件详情
     * @param fileContent 文件原始内容（字节数组）
     * @param splitConfig 分段配置
     * @return 处理后的段落列表（保留原始格式）
     */
    List<ProcessedSegment> parseAndSplit(FileDetail fileDetail, byte[] fileContent, SegmentSplitConfig splitConfig);
    
    /**
     * 阶段2：翻译增强与智能分割
     * 对特殊节点（代码块、表格等）进行翻译，并进行智能二次分割
     * 
     * @param documentTree 文档树（原文）
     * @param splitConfig 分段配置
     * @return 翻译并分割后的段落列表
     */
    List<ProcessedSegment> translateAndSmartSplit(DocumentTree documentTree, SegmentSplitConfig splitConfig);
    
    /**
     * 获取支持的文件扩展名
     * 
     * @return 扩展名列表（如：md, pdf, docx）
     */
    List<String> getSupportedExtensions();
    
    /**
     * 获取策略名称
     * 
     * @return 策略名称
     */
    String getStrategyName();
}
