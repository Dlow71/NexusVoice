package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.infrastructure.persistence.converter.AiModelPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.AiModelPOMapper;
import com.nexusvoice.infrastructure.persistence.po.AiModelPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AI模型仓储实现
 * 使用PO层进行持久化操作
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Repository
public class AiModelRepositoryImpl implements AiModelRepository {
    
    private final AiModelPOMapper mapper;
    private final AiModelPOConverter converter;
    
    public AiModelRepositoryImpl(AiModelPOMapper mapper, AiModelPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }
    
    @Override
    public Optional<AiModel> findById(Long id) {
        AiModelPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public Optional<AiModel> findByProviderAndModel(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPO::getProviderCode, providerCode)
                .eq(AiModelPO::getModelCode, modelCode)
                .eq(AiModelPO::getDeleted, 0);
        
        AiModelPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
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
        LambdaQueryWrapper<AiModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPO::getStatus, 1)
                .eq(AiModelPO::getDeleted, 0)
                .orderByAsc(AiModelPO::getPriority);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiModel> findByTypeEnabled(String modelType) {
        LambdaQueryWrapper<AiModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPO::getModelType, modelType)
                .eq(AiModelPO::getStatus, 1)
                .eq(AiModelPO::getDeleted, 0)
                .orderByAsc(AiModelPO::getPriority);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AiModel> findByProvider(String providerCode) {
        LambdaQueryWrapper<AiModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPO::getProviderCode, providerCode)
                .eq(AiModelPO::getDeleted, 0)
                .orderByAsc(AiModelPO::getPriority);
        
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public AiModel save(AiModel model) {
        AiModelPO po = converter.toPO(model);
        if (po.getId() == null) {
            // 新增
            po.setCreatedAt(LocalDateTime.now());
            mapper.insert(po);
            model.setId(po.getId());
        } else {
            // 更新
            po.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(po);
        }
        return converter.toDomain(po);
    }
    
    @Override
    public void saveAll(List<AiModel> models) {
        for (AiModel model : models) {
            save(model);
        }
    }
    
    @Override
    public void updateStatus(Long id, Integer status) {
        AiModelPO po = new AiModelPO();
        po.setId(id);
        po.setStatus(status);
        po.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(po);
        
        log.info("更新AI模型状态，ID：{}，状态：{}", id, status);
    }
    
    @Override
    public void delete(Long id) {
        AiModelPO po = new AiModelPO();
        po.setId(id);
        po.setDeleted(1);
        po.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(po);
        
        log.info("逻辑删除AI模型，ID：{}", id);
    }
    
    @Override
    public boolean exists(String providerCode, String modelCode) {
        LambdaQueryWrapper<AiModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPO::getProviderCode, providerCode)
                .eq(AiModelPO::getModelCode, modelCode)
                .eq(AiModelPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper) > 0;
    }
    
    @Override
    public List<String> findAllProviderCodes() {
        LambdaQueryWrapper<AiModelPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelPO::getDeleted, 0)
                .select(AiModelPO::getProviderCode)
                .groupBy(AiModelPO::getProviderCode);
        
        List<AiModelPO> pos = mapper.selectList(wrapper);
        return pos.stream()
                .map(AiModelPO::getProviderCode)
                .distinct()
                .toList();
    }
}
