package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.infrastructure.database.mapper.AiModelMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI模型仓储实现
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Repository
public class AiModelRepositoryImpl implements AiModelRepository {
    
    @Autowired
    private AiModelMapper aiModelMapper;
    
    @Override
    public Optional<AiModel> findById(Long id) {
        AiModel model = aiModelMapper.selectById(id);
        return Optional.ofNullable(model);
    }
    
    @Override
    public Optional<AiModel> findByProviderAndModel(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getProviderCode, providerCode)
                .eq(AiModel::getModelCode, modelCode)
                .eq(AiModel::getDeleted, 0);
        
        AiModel model = aiModelMapper.selectOne(wrapper);
        return Optional.ofNullable(model);
    }
    
    @Override
    public Optional<AiModel> findByModelKey(String modelKey) {
        if (modelKey == null || !modelKey.contains(":")) {
            return Optional.empty();
        }
        
        String[] parts = modelKey.split(":", 2);
        return findByProviderAndModel(parts[0], parts[1]);
    }
    
    @Override
    public List<AiModel> findAllEnabled() {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getStatus, 1)
                .eq(AiModel::getDeleted, 0)
                .orderByAsc(AiModel::getPriority);
        
        return aiModelMapper.selectList(wrapper);
    }
    
    @Override
    public List<AiModel> findByProvider(String providerCode) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getProviderCode, providerCode)
                .eq(AiModel::getDeleted, 0)
                .orderByAsc(AiModel::getPriority);
        
        return aiModelMapper.selectList(wrapper);
    }
    
    @Override
    public AiModel save(AiModel model) {
        if (model.getId() == null) {
            // 新增
            model.setCreatedAt(LocalDateTime.now());
            aiModelMapper.insert(model);
        } else {
            // 更新
            model.setUpdatedAt(LocalDateTime.now());
            aiModelMapper.updateById(model);
        }
        return model;
    }
    
    @Override
    public void saveAll(List<AiModel> models) {
        for (AiModel model : models) {
            save(model);
        }
    }
    
    @Override
    public void updateStatus(Long id, Integer status) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setStatus(status);
        model.setUpdatedAt(LocalDateTime.now());
        aiModelMapper.updateById(model);
        
        log.info("更新AI模型状态，ID：{}，状态：{}", id, status);
    }
    
    @Override
    public void delete(Long id) {
        AiModel model = new AiModel();
        model.setId(id);
        model.setDeleted(1);
        model.setUpdatedAt(LocalDateTime.now());
        aiModelMapper.updateById(model);
        
        log.info("逻辑删除AI模型，ID：{}", id);
    }
    
    @Override
    public boolean exists(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getProviderCode, providerCode)
                .eq(AiModel::getModelCode, modelCode)
                .eq(AiModel::getDeleted, 0);
        
        return aiModelMapper.selectCount(wrapper) > 0;
    }
    
    @Override
    public List<String> findAllProviderCodes() {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModel::getDeleted, 0)
                .select(AiModel::getProviderCode)
                .groupBy(AiModel::getProviderCode);
        
        List<AiModel> models = aiModelMapper.selectList(wrapper);
        return models.stream()
                .map(AiModel::getProviderCode)
                .distinct()
                .toList();
    }
}
