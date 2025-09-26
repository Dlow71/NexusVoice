package com.nexusvoice.interfaces.api.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.application.user.dto.*;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.user.constant.UserStatus;
import com.nexusvoice.domain.user.constant.UserType;
import com.nexusvoice.domain.user.model.User;
import com.nexusvoice.domain.user.repository.UserRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员用户管理控制器
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Tag(name = "管理员-用户管理", description = "管理员用户管理相关接口")
@RestController
@RequestMapping("/api/admin/users")
@RequireAdmin
public class AdminUserController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 获取用户列表
     */
    @Operation(summary = "获取用户列表", description = "分页获取用户列表，支持搜索和筛选")
    @GetMapping
    public Result<Page<UserListResponse>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "用户类型") @RequestParam(required = false) UserType userType) {
        
        log.info("获取用户列表: page={}, size={}, keyword={}, status={}, userType={}", 
                page, size, keyword, status, userType);
        
        com.nexusvoice.application.user.dto.PageResult<User> userPage = userRepository.findUserPage(page, size, keyword, userType, status);
        
        // 转换为响应DTO
        Page<UserListResponse> responsePage = new Page<>(page, size, userPage.getTotal());
        List<UserListResponse> responseList = userPage.getRecords().stream()
                .map(user -> new UserListResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getNickname(),
                        user.getAvatarUrl(),
                        user.getUserType(),
                        user.getStatus(),
                        user.getEmailVerified(),
                        user.getLastLoginAt(),
                        user.getCreatedAt()
                ))
                .toList();
        responsePage.setRecords(responseList);
        
        return Result.success("获取用户列表成功", responsePage);
    }
    
    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{userId}")
    public Result<UserDetailResponse> getUserDetail(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        
        log.info("获取用户详情: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        UserDetailResponse response = new UserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getUserType(),
                user.getStatus(),
                user.getEmailVerified(),
                user.getProfileBio(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        return Result.success("获取用户详情成功", response);
    }
    
    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    @PostMapping
    public Result<Void> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("创建用户: {}", request);
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BizException.of(ErrorCodeEnum.USER_EMAIL_EXISTS, "该邮箱已被注册");
        }
        
        // 检查手机号是否已存在（如果提供）
        if (StringUtils.hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw BizException.of(ErrorCodeEnum.USER_PHONE_EXISTS, "该手机号已被使用");
        }
        
        // 创建用户
        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname()
        );
        
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }
        
        userRepository.save(user);
        log.info("用户创建成功: userId={}", user.getId());
        
        return Result.success("用户创建成功");
    }
    
    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "管理员更新用户信息")
    @PutMapping("/{userId}")
    public Result<Void> updateUser(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request) {
        
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
        
        // 更新手机号（需要验证唯一性）
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw BizException.of(ErrorCodeEnum.USER_PHONE_EXISTS, "该手机号已被使用");
            }
            user.setPhone(request.getPhone());
        }
        
        // 更新用户类型
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        
        // 更新邮箱验证状态
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }
        
        // 更新简介
        if (request.getProfileBio() != null) {
            user.setProfileBio(request.getProfileBio());
        }
        
        userRepository.update(user);
        log.info("用户信息更新成功: userId={}", userId);
        
        return Result.success("用户信息更新成功");
    }
    
    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @PutMapping("/{userId}/password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        
        log.info("重置用户密码: userId={}", userId);
        
        String newPassword = request.get("newPassword");
        if (!StringUtils.hasText(newPassword)) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "新密码不能为空");
        }
        
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "密码长度必须在6-20个字符之间");
        }
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.update(user);
        
        log.info("用户密码重置成功: userId={}", userId);
        return Result.success("密码重置成功");
    }
    
    /**
     * 封禁用户
     */
    @Operation(summary = "封禁用户", description = "封禁指定用户")
    @PutMapping("/{userId}/ban")
    public Result<Void> banUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("封禁用户: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        user.ban();
        userRepository.update(user);
        
        log.info("用户封禁成功: userId={}", userId);
        return Result.success("用户封禁成功");
    }
    
    /**
     * 解封用户
     */
    @Operation(summary = "解封用户", description = "解封指定用户")
    @PutMapping("/{userId}/unban")
    public Result<Void> unbanUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("解封用户: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        user.unban();
        userRepository.update(user);
        
        log.info("用户解封成功: userId={}", userId);
        return Result.success("用户解封成功");
    }
    
    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "软删除指定用户")
    @DeleteMapping("/{userId}")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        log.info("删除用户: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        userRepository.deleteById(userId);
        log.info("用户删除成功: userId={}", userId);
        
        return Result.success("用户删除成功");
    }
}
