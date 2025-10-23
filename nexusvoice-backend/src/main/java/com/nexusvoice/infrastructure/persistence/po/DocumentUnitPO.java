package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 文档单元持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_units")
public class DocumentUnitPO extends BasePO {

    /**
     * 关联文件ID
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 单元类型：TEXT-文本，TABLE-表格，IMAGE-图片描述
     */
    @TableField("unit_type")
    private String unitType;

    /**
     * 文本内容
     */
    @TableField("content")
    private String content;

    /**
     * 页码（从1开始）
     */
    @TableField("page_number")
    private Integer page;

    /**
     * 段落索引
     */
    @TableField("paragraph_index")
    private Integer paragraphIndex;

    /**
     * 分块索引（同一页可能多个块）
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * 在原文中的起始位置
     */
    @TableField("start_position")
    private Integer startPosition;

    /**
     * 在原文中的结束位置
     */
    @TableField("end_position")
    private Integer endPosition;

    /**
     * 字符数
     */
    @TableField("char_count")
    private Integer charCount;

    /**
     * Token数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 是否OCR处理
     */
    @TableField("is_ocr")
    private Boolean isOcr;

    /**
     * OCR识别置信度
     */
    @TableField("ocr_confidence")
    private BigDecimal ocrConfidence;

    /**
     * 是否已向量化
     */
    @TableField("is_vectorized")
    private Boolean isVector;

    /**
     * 语言代码：zh/en/ja等
     */
    @TableField("language")
    private String language;

    /**
     * 元数据：标题、作者、关键词等（JSON格式）
     */
    @TableField("metadata")
    private String metadata;
}
