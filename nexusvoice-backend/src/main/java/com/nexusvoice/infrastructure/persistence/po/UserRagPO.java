package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 用户安装的RAG持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_rags")
public class UserRagPO extends BasePO {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联的版本快照ID
     */
    @TableField("rag_version_id")
    private Long ragVersionId;

    /**
     * 原始知识库ID
     */
    @TableField("original_knowledge_base_id")
    private Long originalKnowledgeBaseId;

    /**
     * 安装时的名称
     */
    @TableField("name")
    private String name;

    /**
     * 安装时的图标URL
     */
    @TableField("icon")
    private String icon;

    /**
     * 安装时的描述
     */
    @TableField("description")
    private String description;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

    /**
     * 安装类型：REFERENCE-引用，SNAPSHOT-快照
     */
    @TableField("install_type")
    private String installType;

    /**
     * 安装时间
     */
    @TableField("installed_at")
    private LocalDateTime installedAt;
}
