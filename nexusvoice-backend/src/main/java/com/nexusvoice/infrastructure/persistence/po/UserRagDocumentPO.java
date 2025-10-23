package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户RAG文档快照持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_rag_documents")
public class UserRagDocumentPO extends BasePO {

    /**
     * 关联的用户RAG ID
     */
    @TableField("user_rag_id")
    private Long userRagId;

    /**
     * 关联的用户RAG文件ID
     */
    @TableField("user_rag_file_id")
    private Long userRagFileId;

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
