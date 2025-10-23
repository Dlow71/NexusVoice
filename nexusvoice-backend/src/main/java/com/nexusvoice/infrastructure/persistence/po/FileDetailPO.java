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
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件页数
     */
    @TableField("file_page_size")
    private Integer filePageSize;

    /**
     * 文件类型：PDF,WORD,TXT,EXCEL,PPT,HTML,MARKDOWN,CSV,JSON,XML
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件存储路径
     */
    @TableField("file_path")
    private String filePath;

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
     * 处理状态：0-上传中，1-解析中，2-向量化中，3-完成，4-失败
     */
    @TableField("process_status")
    private Integer processStatus;

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
    @TableField("metadata")
    private String metadata;
}
