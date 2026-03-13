package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件详情持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_details")
public class FileDetailPO extends BasePO {

    /**
     * 上传用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 所属知识库ID
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 存储文件名
     */
    @TableField("filename")
    private String filename;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件页数
     */
    @TableField("file_page_count")
    private Integer filePageCount;

    /**
     * 文件类型：PDF,WORD,TXT,EXCEL,PPT,HTML,MARKDOWN,CSV,JSON,XML
     */
    @TableField("file_type")
    private String fileType;

    /**
     * MIME类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 存储提供商
     */
    @TableField("storage_provider")
    private String storageProvider;

    /**
     * 存储Key
     */
    @TableField("storage_key")
    private String storageKey;

    /**
     * 存储URL
     */
    @TableField("storage_url")
    private String storageUrl;

    /**
     * 文件哈希值
     */
    @TableField("file_hash")
    private String fileHash;

    /**
     * 解析策略：AUTO,OCR,NATIVE,CUSTOM
     */
    @TableField("parse_strategy")
    private String parseStrategy;

    /**
     * 处理状态：PENDING/UPLOADING/PARSING/SPLITTING/VECTORIZING/COMPLETED/FAILED
     */
    @TableField("status")
    private String status;

    /**
     * 当前处理页数
     */
    @TableField("current_process_page")
    private Integer currentProcessPage;

    /**
     * 处理进度（%）
     */
    @TableField("process_progress")
    private java.math.BigDecimal processProgress;

    /**
     * 错误码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误消息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 元数据（JSON格式）
     */
    @TableField(exist = false)
    private String metadata;

    /**
     * 处理完成时间
     */
    @TableField("processed_at")
    private java.time.LocalDateTime processedAt;
}
