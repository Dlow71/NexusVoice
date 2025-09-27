package com.nexusvoice.application.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "创建角色请求")
public class RoleCreateRequest {

    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不能超过50个字符")
    private String name;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "人设提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "人设提示词不能为空")
    private String personaPrompt;

    @Schema(description = "开场白文本")
    @Size(max = 255, message = "开场白文本不能超过255个字符")
    private String greetingMessage;

    @Schema(description = "开场白音频URL（可选，如果不提供将根据greetingMessage自动生成）")
    @Size(max = 255, message = "开场白音频URL不能超过255个字符")
    private String greetingAudioUrl;

    @Schema(description = "头像URL")
    @Size(max = 255, message = "头像URL不能超过255个字符")
    private String avatarUrl;

    @Schema(description = "TTS声音类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "声音类型不能为空")
    @Size(max = 50, message = "声音类型不能超过50个字符")
    private String voiceType;

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
}
