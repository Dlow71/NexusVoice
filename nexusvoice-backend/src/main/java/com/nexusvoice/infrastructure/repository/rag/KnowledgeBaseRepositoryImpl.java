package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.KnowledgeBase;
import com.nexusvoice.domain.rag.model.enums.KnowledgeBaseStatus;
import com.nexusvoice.domain.rag.repository.KnowledgeBaseRepository;
import com.nexusvoice.infrastructure.persistence.converter.KnowledgeBasePOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.KnowledgeBasePOMapper;
import com.nexusvoice.infrastructure.persistence.po.KnowledgeBasePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 知识库仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private final KnowledgeBasePOMapper mapper;
    private final KnowledgeBasePOConverter converter;

    public KnowledgeBaseRepositoryImpl(KnowledgeBasePOMapper mapper, KnowledgeBasePOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        KnowledgeBasePO po = converter.toPO(knowledgeBase);
        if (knowledgeBase.getId() == null) {
            mapper.insert(po);
            knowledgeBase.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return knowledgeBase;
    }

    @Override
    public Optional<KnowledgeBase> findById(Long id) {
        KnowledgeBasePO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<KnowledgeBase> findByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeBasePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBasePO::getUserId, userId)
               .eq(KnowledgeBasePO::getDeleted, 0)
               .orderByDesc(KnowledgeBasePO::getCreatedAt);
        
        List<KnowledgeBasePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeBase> findByUserIdAndStatus(Long userId, KnowledgeBaseStatus status) {
        LambdaQueryWrapper<KnowledgeBasePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBasePO::getUserId, userId)
               .eq(KnowledgeBasePO::getStatus, status.name())
               .eq(KnowledgeBasePO::getDeleted, 0)
               .orderByDesc(KnowledgeBasePO::getCreatedAt);
        
        List<KnowledgeBasePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<KnowledgeBase> findByUserIdAndName(Long userId, String name) {
        LambdaQueryWrapper<KnowledgeBasePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBasePO::getUserId, userId)
               .eq(KnowledgeBasePO::getName, name)
               .eq(KnowledgeBasePO::getDeleted, 0);
        
        KnowledgeBasePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public KnowledgeBase update(KnowledgeBase knowledgeBase) {
        KnowledgeBasePO po = converter.toPO(knowledgeBase);
        mapper.updateById(po);
        return knowledgeBase;
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public int countByUserId(Long userId) {
        LambdaQueryWrapper<KnowledgeBasePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBasePO::getUserId, userId)
               .eq(KnowledgeBasePO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public boolean existsByUserIdAndName(Long userId, String name) {
        LambdaQueryWrapper<KnowledgeBasePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBasePO::getUserId, userId)
               .eq(KnowledgeBasePO::getName, name)
               .eq(KnowledgeBasePO::getDeleted, 0);
        
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public void incrementFileCount(Long id, Long fileSize) {
        KnowledgeBasePO existingPo = mapper.selectById(id);
        if (existingPo != null) {
            KnowledgeBasePO po = new KnowledgeBasePO();
            po.setId(id);
            po.setFileCount((existingPo.getFileCount() != null ? existingPo.getFileCount() : 0) + 1);
            po.setTotalSize((existingPo.getTotalSize() != null ? existingPo.getTotalSize() : 0L) + fileSize);
            mapper.updateById(po);
        }
    }

    @Override
    public void decrementFileCount(Long id, Long fileSize) {
        KnowledgeBasePO existingPo = mapper.selectById(id);
        if (existingPo != null) {
            KnowledgeBasePO po = new KnowledgeBasePO();
            po.setId(id);
            int newFileCount = (existingPo.getFileCount() != null ? existingPo.getFileCount() : 0) - 1;
            po.setFileCount(Math.max(newFileCount, 0));
            
            long newTotalSize = (existingPo.getTotalSize() != null ? existingPo.getTotalSize() : 0L) - fileSize;
            po.setTotalSize(Math.max(newTotalSize, 0L));
            mapper.updateById(po);
        }
    }

    @Override
    public void updateStatus(Long id, KnowledgeBaseStatus status) {
        KnowledgeBasePO po = new KnowledgeBasePO();
        po.setId(id);
        po.setStatus(status.name());
        mapper.updateById(po);
    }
}
