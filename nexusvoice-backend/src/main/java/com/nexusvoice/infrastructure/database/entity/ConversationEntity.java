package com.nexusvoice.infrastructure.database.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.nexusvoice.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对话数据库实体
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversations")
public class ConversationEntity extends BaseEntity {

    /**
     * 对话标题
     */
    @TableField("title")
    private String title;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色ID（可为空）
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * AI模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 对话状态
     */
    @TableField("status")
    private String status;

    /**
     * 系统提示词
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 对话配置参数（JSON格式）
     */
    @TableField("config_params")
    private String configParams;

    /**
     * 最后活跃时间
     */
    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;
}
