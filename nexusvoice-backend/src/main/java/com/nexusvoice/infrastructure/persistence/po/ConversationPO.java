package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对话持久化对象
 * 对应数据库表conversations
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("conversations")
public class ConversationPO extends BasePO {

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
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * AI模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 对话状态（ACTIVE/ARCHIVED/DELETED）
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
