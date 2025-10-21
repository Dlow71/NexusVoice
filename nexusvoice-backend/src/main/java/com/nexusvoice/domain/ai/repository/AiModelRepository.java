package com.nexusvoice.domain.ai.repository;

import com.nexusvoice.domain.ai.model.AiModel;
import java.util.List;
import java.util.Optional;

/**
 * AI模型仓储接口
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
public interface AiModelRepository {
    
    /**
     * 根据ID查询模型
     */
    Optional<AiModel> findById(Long id);
    
    /**
     * 根据厂商和模型代码查询
     */
    Optional<AiModel> findByProviderAndModel(String providerCode, String modelCode);
    
    /**
     * 根据模型键查询（provider:model格式）
     */
    Optional<AiModel> findByModelKey(String modelKey);
    
    /**
     * 查询所有启用的模型
     */
    List<AiModel> findAllEnabled();
    
    /**
     * 查询指定类型的所有启用模型
     */
    List<AiModel> findByTypeEnabled(String modelType);
    
    /**
     * 查询指定厂商的所有模型
     */
    List<AiModel> findByProvider(String providerCode);
    
    /**
     * 保存模型
     */
    AiModel save(AiModel model);
    
    /**
     * 批量保存
     */
    void saveAll(List<AiModel> models);
    
    /**
     * 更新模型状态
     */
    void updateStatus(Long id, Integer status);
    
    /**
     * 删除模型（逻辑删除）
     */
    void delete(Long id);
    
    /**
     * 检查模型是否存在
     */
    boolean exists(String providerCode, String modelCode);
    
    /**
     * 获取所有提供商代码
     */
    List<String> findAllProviderCodes();
}
