package com.nexusvoice.application.user.dto;

import com.nexusvoice.domain.user.constant.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户请求DTO
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Schema(description = "更新用户请求")
public class UpdateUserRequest {

    @Schema(description = "昵称", example = "新昵称")
    @Size(min = 2, max = 50, message = "昵称长度必须在2-50个字符之间")
    private String nickname;

    @Schema(description = "头像URL")
    @Size(max = 255, message = "头像URL不能超过255个字符")
    private String avatarUrl;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "用户类型")
    private UserType userType;

    @Schema(description = "用户简介")
    @Size(max = 500, message = "用户简介不能超过500个字符")
    private String profileBio;

    @Schema(description = "邮箱是否已验证")
    private Boolean emailVerified;

    // 构造函数
    public UpdateUserRequest() {}

    // Getter and Setter methods
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getProfileBio() {
        return profileBio;
    }

    public void setProfileBio(String profileBio) {
        this.profileBio = profileBio;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Override
    public String toString() {
        return "UpdateUserRequest{" +
                "nickname='" + nickname + '\'' +
                ", phone='" + phone + '\'' +
                ", userType=" + userType +
                '}';
    }
}
