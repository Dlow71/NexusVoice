package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * RAG版本快照持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rag_versions")
public class RagVersionPO extends BasePO {

    /**
     * 原始知识库ID
     */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 创建者用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 快照时的名称
     */
    @TableField("name")
    private String name;

    /**
     * 快照时的图标URL
     */
    @TableField("icon")
    private String icon;

    /**
     * 快照时的描述
     */
    @TableField("description")
    private String description;

    /**
     * 版本号（语义化版本：1.0.0、1.1.0等）
     */
    @TableField("version")
    private String version;

    /**
     * 更新日志
     */
    @TableField("change_log")
    private String changeLog;

    /**
     * 标签（JSON字符串格式）
     */
    @TableField("labels")
    private String labels;

    /**
     * 文件数量
     */
    @TableField("file_count")
    private Integer fileCount;

    /**
     * 总大小（字节）
     */
    @TableField("total_size")
    private Long totalSize;

    /**
     * 文档单元数量
     */
    @TableField("document_count")
    private Integer documentCount;

    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;
}
