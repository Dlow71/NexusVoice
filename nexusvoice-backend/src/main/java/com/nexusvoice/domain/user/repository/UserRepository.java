package com.nexusvoice.domain.user.repository;

import com.nexusvoice.domain.user.model.User;

import java.util.Optional;

/**
 * 用户仓储接口
 * 定义用户数据访问的领域接口
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
public interface UserRepository {

    /**
     * 根据ID查找用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    Optional<User> findById(String id);

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否已存在
     *
     * @param phone 手机号
     * @return 是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 保存用户
     *
     * @param user 用户信息
     * @return 保存后的用户信息
     */
    User save(User user);

    /**
     * 更新用户
     *
     * @param user 用户信息
     * @return 更新后的用户信息
     */
    User update(User user);

    /**
     * 根据ID删除用户（逻辑删除）
     *
     * @param id 用户ID
     */
    void deleteById(String id);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    long count();

    /**
     * 分页查询用户列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @param userType 用户类型
     * @param status 用户状态
     * @return 分页结果
     */
    com.nexusvoice.application.user.dto.PageResult<User> findUserPage(
            Integer page, Integer size, String keyword, 
            com.nexusvoice.domain.user.constant.UserType userType, 
            com.nexusvoice.domain.user.constant.UserStatus status);
}
