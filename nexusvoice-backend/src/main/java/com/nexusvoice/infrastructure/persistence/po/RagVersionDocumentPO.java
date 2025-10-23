package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * RAG版本文档快照持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rag_version_documents")
public class RagVersionDocumentPO extends BasePO {

    /**
     * 关联的版本ID
     */
    @TableField("rag_version_id")
    private Long ragVersionId;

    /**
     * 关联的版本文件ID
     */
    @TableField("rag_version_file_id")
    private Long ragVersionFileId;

    /**
     * 原始文档单元ID（仅标识用）
     */
    @TableField("original_document_id")
    private Long originalDocumentId;

    /**
     * 文档内容
     */
    @TableField("content")
    private String content;

    /**
     * 页码
     */
    @TableField("page")
    private Integer page;

    /**
     * 向量ID（在pgvector中的ID）
     */
    @TableField("vector_id")
    private String vectorId;
}
