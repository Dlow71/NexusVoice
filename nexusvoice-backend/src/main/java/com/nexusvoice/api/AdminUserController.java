package com.nexusvoice.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    @GetMapping
    public Result<PageResult<UserInfo>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户类型") @RequestParam(required = false) UserType userType,
            @Parameter(description = "用户状态") @RequestParam(required = false) UserStatus status) {
        
        log.info("获取用户列表: page={}, size={}, keyword={}, userType={}, status={}", 
                page, size, keyword, userType, status);
        
        // 使用Repository进行分页查询
        PageResult<User> userPageResult = userRepository.findUserPage(page, size, keyword, userType, status);
        
        // 转换为UserInfo DTO
        List<UserInfo> userInfos = userPageResult.getRecords().stream()
                .map(this::convertToUserInfo)
                .collect(java.util.stream.Collectors.toList());
        
        PageResult<UserInfo> result = new PageResult<>(
                userInfos, 
                userPageResult.getTotal(), 
                userPageResult.getCurrent(), 
                userPageResult.getSize());
        
        return Result.success("获取用户列表成功", result);
    }
    
    /**
     * 获取用户详情
     */
    @Operation(summary = "获取用户详情", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
    public Result<UserInfo> getUserDetail(@PathVariable String id) {
        log.info("获取用户详情: id={}", id);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        UserInfo userInfo = convertToUserInfo(user);
        
        return Result.success("获取用户详情成功", userInfo);
    }
    
    /**
     * 创建用户
     */
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    @PostMapping
    public Result<UserInfo> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("创建用户: {}", request);
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BizException.of(ErrorCodeEnum.USER_EMAIL_EXISTS, "该邮箱已被注册");
        }
        
        // 检查手机号是否已存在
        if (StringUtils.hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw BizException.of(ErrorCodeEnum.USER_PHONE_EXISTS, "该手机号已被注册");
        }
        
        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setUserType(request.getUserType() != null ? request.getUserType() : UserType.USER);
        user.setStatus(UserStatus.NORMAL);
        user.setEmailVerified(false);
        user.setProfileBio(request.getProfileBio());
        
        User savedUser = userRepository.save(user);
        UserInfo userInfo = convertToUserInfo(savedUser);
        
        log.info("用户创建成功: id={}", savedUser.getId());
        return Result.success("用户创建成功", userInfo);
    }
    
    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "管理员更新用户信息")
    @PutMapping("/{id}")
    public Result<UserInfo> updateUser(@PathVariable String id, 
                                      @Valid @RequestBody UpdateUserRequest request) {
        log.info("更新用户信息: id={}, request={}", id, request);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        
        // 更新基本信息
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        
        if (request.getProfileBio() != null) {
            user.setProfileBio(request.getProfileBio());
        }
        
        // 更新用户类型
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());
        }
        
        // 更新手机号（需要验证唯一性）
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw BizException.of(ErrorCodeEnum.USER_PHONE_EXISTS, "该手机号已被使用");
            }
            user.setPhone(request.getPhone());
        }
        
        User updatedUser = userRepository.update(user);
        UserInfo userInfo = convertToUserInfo(updatedUser);
        
        log.info("用户信息更新成功: id={}", id);
        return Result.success("用户信息更新成功", userInfo);
    }
    
    /**
     * 更新用户状态
     */
    @Operation(summary = "更新用户状态", description = "封禁或解封用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable String id, 
                                        @RequestBody Map<String, Object> request) {
        log.info("更新用户状态: id={}, request={}", id, request);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        
        // 获取新状态
        Integer statusCode = (Integer) request.get("status");
        if (statusCode == null) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "状态参数不能为空");
        }
        
        UserStatus newStatus = UserStatus.fromCode(statusCode);
        user.setStatus(newStatus);
        
        userRepository.update(user);
        
        String action = newStatus == UserStatus.BANNED ? "封禁" : "解封";
        log.info("用户状态更新成功: id={}, action={}", id, action);
        
        return Result.success("用户" + action + "成功");
    }
    
    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @PutMapping("/{id}/password")
    public Result<Map<String, String>> resetUserPassword(@PathVariable String id) {
        log.info("重置用户密码: id={}", id);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        User user = userOpt.get();
        
        // 生成临时密码
        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        
        userRepository.update(user);
        
        log.info("用户密码重置成功: id={}", id);
        return Result.success("密码重置成功", Map.of("tempPassword", tempPassword));
    }
    
    /**
     * 删除用户
     */
    @Operation(summary = "删除用户", description = "逻辑删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable String id) {
        log.info("删除用户: id={}", id);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.USER_NOT_FOUND, "用户不存在");
        }
        
        userRepository.deleteById(id);
        
        log.info("用户删除成功: id={}", id);
        return Result.success("用户删除成功");
    }
    
    /**
     * 转换为用户信息DTO
     */
    private UserInfo convertToUserInfo(User user) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setPhone(user.getPhone());
        userInfo.setUserType(user.getUserType());
        userInfo.setStatus(user.getStatus());
        userInfo.setEmailVerified(user.getEmailVerified());
        userInfo.setLastLoginAt(user.getLastLoginAt());
        userInfo.setProfileBio(user.getProfileBio());
        userInfo.setCreatedAt(user.getCreatedAt());
        userInfo.setUpdatedAt(user.getUpdatedAt());
        return userInfo;
    }
    
    /**
     * 生成临时密码
     */
    private String generateTempPassword() {
        return "temp" + System.currentTimeMillis() % 100000;
    }
}
