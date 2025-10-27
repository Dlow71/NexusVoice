package com.nexusvoice.domain.auth.repository;

import com.nexusvoice.domain.auth.model.TokenBlacklist;

import java.util.List;

/**
 * Token黑名单仓储接口
 * 纯领域层接口，不依赖任何基础设施
 *
 * @author NexusVoice
 * @since 2025-01-27
 */
public interface TokenBlacklistRepository {
    
    /**
     * 添加Token到黑名单
     *
     * @param tokenBlacklist Token黑名单记录
     */
    void add(TokenBlacklist tokenBlacklist);
    
    /**
     * 批量添加Token到黑名单
     *
     * @param tokens Token列表
     * @param userId 用户ID
     * @param reason 原因
     */
    void addBatch(List<String> tokens, Long userId, String reason);
    
    /**
     * 检查Token是否在黑名单中
     *
     * @param token Token
     * @return 是否在黑名单中
     */
    boolean isBlacklisted(String token);
    
    /**
     * 从黑名单中移除Token
     *
     * @param token Token
     */
    void remove(String token);
    
    /**
     * 获取用户的所有黑名单Token
     *
     * @param userId 用户ID
     * @return 黑名单记录列表
     */
    List<TokenBlacklist> findByUserId(Long userId);
    
    /**
     * 清理过期的黑名单记录
     *
     * @return 清理的记录数量
     */
    long cleanExpired();
}
