package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 向量存储持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 注意：此表使用UUID作为主键，不继承BasePO
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@TableName("vector_store")
public class VectorStorePO {

    /**
     * 主键ID（UUID）
     */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 关联的文档单元ID
     */
    @TableField("document_unit_id")
    private Long documentUnitId;

    /**
     * 向量模型名称
     */
    @TableField("embedding_model")
    private String embeddingModel;

    /**
     * 向量维度
     */
    @TableField("embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 向量数据（PostgreSQL的vector类型在Java中用List<Float>表示）
     */
    @TableField("embedding")
    private List<Float> embedding;

    /**
     * 元数据（JSON格式）
     */
    @TableField("metadata")
    private String metadata;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableField("deleted")
    private Integer deleted;
}
