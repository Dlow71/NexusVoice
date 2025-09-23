package com.nexusvoice.api;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.user.dto.AuthResponse;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.domain.user.repository.UserRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 用户个人信息控制器
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Tag(name = "用户管理", description = "用户个人信息相关接口")
@RestController
@RequestMapping("/api/user")
@RequireAuth
public class UserController {
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 获取个人信息
     */
    @Operation(summary = "获取个人信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/profile")
    public Result<AuthResponse.UserInfo> getProfile() {
        String userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        log.info("获取用户信息: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getUserType(),
                user.getEmailVerified()
        );
        
        return Result.success("获取用户信息成功", userInfo);
    }
    
    /**
     * 更新个人信息
     */
    @Operation(summary = "更新个人信息", description = "更新当前用户的基本信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        String userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        log.info("更新用户信息: userId={}, request={}", userId, request);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        
        // 更新昵称
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        
        // 更新头像
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        // 更新简介
        if (request.getProfileBio() != null) {
            user.setProfileBio(request.getProfileBio());
        }
        
        // 更新手机号（需要验证唯一性）
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw BizException.of(ErrorCodeEnum.USER_PHONE_EXISTS, "该手机号已被使用");
            }
            user.setPhone(request.getPhone());
        }
        
        userRepository.update(user);
        log.info("用户信息更新成功: userId={}", userId);
        
        return Result.success("更新成功");
    }
    
    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "修改当前用户的登录密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        log.info("修改密码: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        
        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw BizException.of(ErrorCodeEnum.USER_PASSWORD_ERROR, "原密码错误");
        }
        
        // 验证新密码确认
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "两次输入的新密码不一致");
        }
        
        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.update(user);
        
        log.info("密码修改成功: userId={}", userId);
        return Result.success("密码修改成功");
    }
    
    /**
     * 上传头像
     */
    @Operation(summary = "上传头像", description = "上传用户头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件URL") @RequestParam String avatarUrl) {
        
        String userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        
        log.info("上传头像: userId={}, avatarUrl={}", userId, avatarUrl);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        user.setAvatarUrl(avatarUrl);
        userRepository.update(user);
        
        return Result.success("头像上传成功", Map.of("avatarUrl", avatarUrl));
    }
    
    /**
     * 更新个人信息请求DTO
     */
    public static class UpdateProfileRequest {
        @Size(min = 2, max = 50, message = "昵称长度必须在2-50个字符之间")
        private String nickname;
        
        @Size(max = 255, message = "头像URL不能超过255个字符")
        private String avatarUrl;
        
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        private String phone;
        
        @Size(max = 500, message = "用户简介不能超过500个字符")
        private String profileBio;
        
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
        
        public String getProfileBio() {
            return profileBio;
        }
        
        public void setProfileBio(String profileBio) {
            this.profileBio = profileBio;
        }
        
        @Override
        public String toString() {
            return "UpdateProfileRequest{" +
                    "nickname='" + nickname + '\'' +
                    ", phone='" + phone + '\'' +
                    ", profileBio='" + profileBio + '\'' +
                    '}';
        }
    }
    
    /**
     * 修改密码请求DTO
     */
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度必须在6-20个字符之间")
        private String newPassword;
        
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;
        
        // Getter and Setter methods
        public String getOldPassword() {
            return oldPassword;
        }
        
        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }
        
        public String getNewPassword() {
            return newPassword;
        }
        
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
        
        public String getConfirmPassword() {
            return confirmPassword;
        }
        
        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
