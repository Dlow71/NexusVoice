package com.nexusvoice.domain.developer.repository;

import com.nexusvoice.domain.developer.model.DeveloperApiKey;

import java.util.List;
import java.util.Optional;

/**
 * 开发者API密钥仓储接口
 * 纯粹的领域层接口，不依赖任何基础设施
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
public interface DeveloperApiKeyRepository {
    
    /**
     * 保存API密钥
     */
    void save(DeveloperApiKey apiKey);
    
    /**
     * 更新API密钥
     */
    void update(DeveloperApiKey apiKey);
    
    /**
     * 根据ID查询API密钥
     */
    Optional<DeveloperApiKey> findById(Long id);
    
    /**
     * 根据key_value_hash查询API密钥（用于认证）
     */
    Optional<DeveloperApiKey> findByKeyValueHash(String keyValueHash);
    
    /**
     * 根据用户ID查询所有API密钥
     */
    List<DeveloperApiKey> findByUserId(Long userId);
    
    /**
     * 根据用户ID和密钥名称查询（检查名称唯一性）
     */
    Optional<DeveloperApiKey> findByUserIdAndKeyName(Long userId, String keyName);
    
    /**
     * 统计用户的API密钥数量
     */
    long countByUserId(Long userId);
    
    /**
     * 逻辑删除API密钥
     */
    void delete(Long id);
    
    /**
     * 批量逻辑删除API密钥
     */
    void batchDelete(List<Long> ids);
    
    /**
     * 查询所有过期的API密钥（用于定时任务清理）
     */
    List<DeveloperApiKey> findExpiredKeys();
    
    /**
     * 查询需要重置今日统计的API密钥（用于定时任务）
     */
    List<DeveloperApiKey> findKeysNeedDailyReset();
}
