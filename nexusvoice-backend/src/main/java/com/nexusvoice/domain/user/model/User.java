package com.nexusvoice.domain.user.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nexusvoice.domain.common.BaseEntity;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户实体类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Schema(description = "用户实体")
@TableName("users")
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    // ID字段继承自BaseEntity (Long类型)

    @Schema(description = "用户登录邮箱")
    @TableField("email")
    private String email;

    @Schema(description = "加密后的用户密码", hidden = true)
    @JsonIgnore
    @TableField("password_hash")
    private String passwordHash;

    @Schema(description = "用户昵称")
    @TableField("nickname")
    private String nickname;

    @Schema(description = "用户头像图片的URL")
    @TableField("avatar_url")
    private String avatarUrl;

    @Schema(description = "手机号码")
    @TableField("phone")
    private String phone;

    @Schema(description = "用户类型")
    @TableField("user_type")
    private UserType userType;

    @Schema(description = "账户状态")
    @TableField("status")
    private UserStatus status;

    @Schema(description = "邮箱是否已验证")
    @TableField("email_verified")
    private Boolean emailVerified;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @Schema(description = "用户简介")
    @TableField("profile_bio")
    private String profileBio;

    // 构造函数
    public User() {
        this.userType = UserType.USER;
        this.status = UserStatus.NORMAL;
        this.emailVerified = false;
    }

    public User(String email, String passwordHash, String nickname) {
        this();
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }

    // 业务方法

    /**
     * 激活用户账户
     */
    public void activate() {
        if (this.status == UserStatus.PENDING_ACTIVATION) {
            this.status = UserStatus.NORMAL;
        }
    }

    /**
     * 封禁用户
     */
    public void ban() {
        if (this.status != UserStatus.BANNED) {
            this.status = UserStatus.BANNED;
        }
    }

    /**
     * 解封用户
     */
    public void unban() {
        if (this.status == UserStatus.BANNED) {
            this.status = UserStatus.NORMAL;
        }
    }

    /**
     * 验证邮箱
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 判断是否为管理员
     *
     * @return true-管理员, false-普通用户
     */
    public boolean isAdmin() {
        return this.userType != null && this.userType.isAdmin();
    }

    /**
     * 判断账户是否正常
     *
     * @return true-正常, false-非正常
     */
    public boolean isAccountNormal() {
        return this.status != null && this.status.isNormal();
    }

    /**
     * 判断是否被封禁
     *
     * @return true-被封禁, false-未被封禁
     */
    public boolean isBanned() {
        return this.status != null && this.status.isBanned();
    }

    /**
     * 判断邮箱是否已验证
     *
     * @return true-已验证, false-未验证
     */
    public boolean hasEmailVerified() {
        return Boolean.TRUE.equals(this.emailVerified);
    }

    // Getter and Setter methods (ID方法继承自BaseEntity)
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getProfileBio() {
        return profileBio;
    }

    public void setProfileBio(String profileBio) {
        this.profileBio = profileBio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", userType=" + userType +
                ", status=" + status +
                ", emailVerified=" + emailVerified +
                ", createdAt=" + createdAt +
                '}';
    }
}
