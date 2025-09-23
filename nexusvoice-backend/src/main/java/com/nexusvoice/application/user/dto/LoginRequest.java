package com.nexusvoice.application.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求DTO
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "邮箱或手机号", example = "user@example.com")
    @NotBlank(message = "邮箱或手机号不能为空")
    private String username;

    @Schema(description = "密码", example = "password123")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "记住我", example = "false")
    private Boolean rememberMe = false;

    // 构造函数
    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public LoginRequest(String username, String password, Boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
    }

    // Getter and Setter methods
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", rememberMe=" + rememberMe +
                '}';
    }
}
