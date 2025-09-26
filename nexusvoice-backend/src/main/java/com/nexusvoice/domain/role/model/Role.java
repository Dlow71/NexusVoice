package com.nexusvoice.domain.role.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nexusvoice.domain.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * AI角色领域实体
 * 对应数据库表 roles
 *
 * 字段说明：
 * - 公共角色：is_public = 1，user_id 为空
 * - 私人角色：is_public = 0，user_id 为创建者ID
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Schema(description = "AI角色实体")
@TableName("roles")
public class Role extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色唯一ID")
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "角色名称")
    @TableField("name")
    private String name;

    @Schema(description = "角色描述")
    @TableField("description")
    private String description;

    @Schema(description = "角色人设提示词")
    @TableField("persona_prompt")
    private String personaPrompt;

    @Schema(description = "开场白文本")
    @TableField("greeting_message")
    private String greetingMessage;

    @Schema(description = "开场白音频URL")
    @TableField("greeting_audio_url")
    private String greetingAudioUrl;

    @Schema(description = "头像URL")
    @TableField("avatar_url")
    private String avatarUrl;

    @Schema(description = "TTS声音类型")
    @TableField("voiceType")
    private String voiceType;

    @Schema(description = "是否公共角色")
    @TableField("is_public")
    private Boolean isPublic;

    @Schema(description = "创建者用户ID（私人角色）")
    @TableField("user_id")
    private String userId;

    // ============ 业务方法 ============

    /**
     * 设为公共角色
     */
    public void makePublic() {
        this.isPublic = true;
        this.userId = null;
    }

    /**
     * 设为私人角色
     * @param ownerUserId 创建者用户ID
     */
    public void makePrivate(String ownerUserId) {
        this.isPublic = false;
        this.userId = ownerUserId;
    }

    /**
     * 判断是否属于指定用户
     */
    public boolean ownedBy(String uid) {
        return uid != null && uid.equals(this.userId);
    }

    // ============ Getter / Setter ============

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPersonaPrompt() {
        return personaPrompt;
    }

    public void setPersonaPrompt(String personaPrompt) {
        this.personaPrompt = personaPrompt;
    }

    public String getGreetingMessage() {
        return greetingMessage;
    }

    public void setGreetingMessage(String greetingMessage) {
        this.greetingMessage = greetingMessage;
    }

    public String getGreetingAudioUrl() {
        return greetingAudioUrl;
    }

    public void setGreetingAudioUrl(String greetingAudioUrl) {
        this.greetingAudioUrl = greetingAudioUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getVoiceType() {
        return voiceType;
    }

    public void setVoiceType(String voiceType) {
        this.voiceType = voiceType;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean aPublic) {
        isPublic = aPublic;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", isPublic=" + isPublic +
                ", userId='" + userId + '\'' +
                '}';
    }
}
