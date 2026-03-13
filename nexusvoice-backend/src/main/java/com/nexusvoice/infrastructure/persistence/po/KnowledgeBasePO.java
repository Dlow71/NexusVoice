package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_bases")
public class KnowledgeBasePO extends BasePO {

    /**
     * 所属用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 知识库名称
     */
    @TableField("name")
    private String name;

    /**
     * 知识库描述
     */
    @TableField("description")
    private String description;

    /**
     * 知识库图标URL
     */
    @TableField("icon")
    private String icon;

    /**
     * 标签（JSON字符串格式）
     */
    @TableField("labels")
    private String labels;

    /**
     * 状态：CREATING-创建中，ACTIVE-可用，PROCESSING-处理中，ERROR-错误，ARCHIVED-已归档
     */
    @TableField("status")
    private String status;

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
    @TableField(exist = false)
    private Integer documentCount;
}
