package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户RAG文件快照持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_rag_files")
public class UserRagFilePO extends BasePO {

    /**
     * 关联的用户RAG ID
     */
    @TableField("user_rag_id")
    private Long userRagId;

    /**
     * 原始文件ID（仅标识用）
     */
    @TableField("original_file_id")
    private Long originalFileId;

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
     * 文件类型
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件存储路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 处理状态
     */
    @TableField("process_status")
    private Integer processStatus;
}
