package com.nexusvoice.domain.auth.repository;

import com.nexusvoice.domain.auth.model.UserSession;

import java.util.List;
import java.util.Optional;

/**
 * 用户会话仓储接口
 * 纯领域层接口，不依赖任何基础设施
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
public interface UserSessionRepository {
    
    /**
     * 保存用户会话
     *
     * @param session 用户会话
     */
    void save(UserSession session);
    
    /**
     * 根据访问令牌查找会话
     *
     * @param accessToken 访问令牌
     * @return 用户会话
     */
    Optional<UserSession> findByAccessToken(String accessToken);
    
    /**
     * 根据刷新令牌查找会话
     *
     * @param refreshToken 刷新令牌
     * @return 用户会话
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<UserSession> findByUserId(Long userId);
    
    /**
     * 删除指定会话（根据访问令牌）
     *
     * @param accessToken 访问令牌
     */
    void deleteByAccessToken(String accessToken);
    
    /**
     * 删除用户的所有会话
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
    
    /**
     * 删除用户的其他会话（保留当前会话）
     *
     * @param userId 用户ID
     * @param currentAccessToken 当前访问令牌
     */
    void deleteOtherSessions(Long userId, String currentAccessToken);
    
    /**
     * 更新会话的最后活跃时间
     *
     * @param accessToken 访问令牌
     */
    void updateLastActive(String accessToken);
    
    /**
     * 统计用户的活跃会话数量
     *
     * @param userId 用户ID
     * @return 会话数量
     */
    long countByUserId(Long userId);
    
    /**
     * 清理过期会话
     *
     * @return 清理的会话数量
     */
    long cleanExpiredSessions();
}
