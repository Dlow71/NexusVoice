package com.nexusvoice.domain.role.model;

import com.nexusvoice.domain.common.BaseDomainEntity;

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
public class Role extends BaseDomainEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 角色人设提示词
     */
    private String personaPrompt;

    /**
     * 开场白文本
     */
    private String greetingMessage;

    /**
     * 开场白音频URL
     */
    private String greetingAudioUrl;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * TTS声音类型
     */
    private String voiceType;

    /**
     * 是否公共角色：0-私有 1-公共
     */
    private Integer isPublic;

    /**
     * 创建者用户ID（私人角色）
     */
    private Long userId;

    // ============ 业务方法 ============

    /**
     * 设为公共角色
     */
    public void makePublic() {
        this.isPublic = 1;
        this.userId = null;
    }

    /**
     * 设为私人角色
     * @param ownerUserId 创建者用户ID
     */
    public void makePrivate(Long ownerUserId) {
        this.isPublic = 0;
        this.userId = ownerUserId;
    }

    /**
     * 判断是否属于指定用户
     */
    public boolean ownedBy(Long uid) {
        return uid != null && uid.equals(this.userId);
    }

    // ============ Getter / Setter ============
    

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

    public Integer getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Integer aPublic) {
        isPublic = aPublic;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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
                ", userId=" + userId +
                '}';
    }
}
