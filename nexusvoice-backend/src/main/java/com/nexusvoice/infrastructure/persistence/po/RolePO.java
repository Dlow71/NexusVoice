package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 角色/Agent持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-21
 * @updated 2025-10-31 V19扩展Agent能力
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("roles")
public class RolePO extends BasePO {

    /**
     * 角色名称
     */
    @TableField("name")
    private String name;

    /**
     * 角色描述
     */
    @TableField("description")
    private String description;

    /**
     * 角色人设提示词
     */
    @TableField("persona_prompt")
    private String personaPrompt;

    /**
     * 开场白文本
     */
    @TableField("greeting_message")
    private String greetingMessage;

    /**
     * 开场白音频URL
     */
    @TableField("greeting_audio_url")
    private String greetingAudioUrl;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * TTS声音类型
     */
    @TableField("voicetype")
    private String voiceType;

    /**
     * 是否公共角色：0-私有 1-公共
     */
    @TableField("is_public")
    private Integer isPublic;

    /**
     * 创建者用户ID（私人角色）
     */
    @TableField("user_id")
    private Long userId;

    // ============ Agent扩展字段（V19） ============

    /**
     * 可用工具ID列表
     */
    @TableField(value = "tool_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> toolIds;

    /**
     * 工具预设参数
     */
    @TableField(value = "tool_preset_params", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> toolPresetParams;

    /**
     * 关联的知识库ID列表
     */
    @TableField(value = "knowledge_base_ids", typeHandler = JacksonTypeHandler.class)
    private List<Long> knowledgeBaseIds;

    /**
     * 是否支持多模态
     */
    @TableField("multi_modal")
    private Boolean multiModal;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 角色标签
     */
    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /**
     * 扩展配置参数
     */
    @TableField(value = "config_params", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> configParams;
}
