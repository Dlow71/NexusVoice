package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-21
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
}
