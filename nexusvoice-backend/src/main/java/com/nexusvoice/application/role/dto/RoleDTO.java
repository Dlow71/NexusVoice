package com.nexusvoice.application.role.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "角色信息DTO")
public class RoleDTO {

    @Schema(description = "角色ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "人设提示词")
    private String personaPrompt;

    @Schema(description = "开场白文本")
    private String greetingMessage;

    @Schema(description = "开场白音频URL")
    private String greetingAudioUrl;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "TTS声音类型")
    private String voiceType;

    @Schema(description = "是否公共角色")
    private Boolean isPublic;

    @Schema(description = "创建者用户ID（私人角色）")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @Schema(description = "创建时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
