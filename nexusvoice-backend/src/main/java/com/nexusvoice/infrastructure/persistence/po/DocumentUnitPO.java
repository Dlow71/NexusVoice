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
     * 文本内容
     */
    @TableField("content")
    private String content;

    /**
     * 页码（从1开始）
     */
    @TableField("page")
    private Integer page;

    /**
     * 是否OCR处理
     */
    @TableField("is_ocr")
    private Boolean isOcr;

    /**
     * 是否已向量化
     */
    @TableField("is_vector")
    private Boolean isVector;
}
