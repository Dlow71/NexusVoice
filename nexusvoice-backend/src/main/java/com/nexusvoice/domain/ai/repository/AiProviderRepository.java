package com.nexusvoice.domain.ai.repository;

import com.nexusvoice.domain.ai.model.AiProvider;

import java.util.List;
import java.util.Optional;

/**
 * AI服务提供商仓储接口
 * 定义服务商的持久化操作契约
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public interface AiProviderRepository {
    
    /**
     * 根据ID查询服务商
     *
     * @param id 服务商ID
     * @return 服务商实体（Optional）
     */
    Optional<AiProvider> findById(Long id);
    
    /**
     * 根据服务商代码查询
     *
     * @param providerCode 服务商代码
     * @return 服务商实体（Optional）
     */
    Optional<AiProvider> findByCode(String providerCode);
    
    /**
     * 查询所有启用的服务商
     *
     * @return 启用的服务商列表
     */
    List<AiProvider> findAllEnabled();
    
    /**
     * 查询所有官方服务商
     *
     * @return 官方服务商列表
     */
    List<AiProvider> findAllOfficial();
    
    /**
     * 查询指定用户的自定义服务商
     *
     * @param userId 用户ID
     * @return 用户自定义服务商列表
     */
    List<AiProvider> findByUserId(Long userId);
    
    /**
     * 查询所有服务商（包括禁用的）
     *
     * @return 所有服务商列表
     */
    List<AiProvider> findAll();
    
    /**
     * 保存或更新服务商
     *
     * @param provider 服务商实体
     * @return 保存后的服务商实体
     */
    AiProvider save(AiProvider provider);
    
    /**
     * 批量保存服务商
     *
     * @param providers 服务商列表
     * @return 保存的数量
     */
    int saveBatch(List<AiProvider> providers);
    
    /**
     * 删除服务商（逻辑删除）
     *
     * @param id 服务商ID
     * @return 是否删除成功
     */
    boolean delete(Long id);
    
    /**
     * 检查服务商代码是否已存在
     *
     * @param providerCode 服务商代码
     * @return true-已存在，false-不存在
     */
    boolean existsByCode(String providerCode);
    
    /**
     * 检查服务商代码是否已存在（排除指定ID）
     *
     * @param providerCode 服务商代码
     * @param excludeId 要排除的ID
     * @return true-已存在，false-不存在
     */
    boolean existsByCodeExcludeId(String providerCode, Long excludeId);
    
    /**
     * 统计服务商数量
     *
     * @param isOfficial 是否官方（null表示不限）
     * @param status 状态（null表示不限）
     * @return 服务商数量
     */
    long count(Boolean isOfficial, Integer status);
    
    /**
     * 更新服务商状态
     *
     * @param id 服务商ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);
}
