package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.RagVersion;
import com.nexusvoice.domain.rag.repository.RagVersionRepository;
import com.nexusvoice.infrastructure.persistence.converter.RagVersionPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.RagVersionPOMapper;
import com.nexusvoice.infrastructure.persistence.po.RagVersionPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RAG版本仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class RagVersionRepositoryImpl implements RagVersionRepository {

    private final RagVersionPOMapper mapper;
    private final RagVersionPOConverter converter;

    public RagVersionRepositoryImpl(RagVersionPOMapper mapper, RagVersionPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public RagVersion save(RagVersion ragVersion) {
        RagVersionPO po = converter.toPO(ragVersion);
        if (ragVersion.getId() == null) {
            mapper.insert(po);
            ragVersion.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return ragVersion;
    }

    @Override
    public Optional<RagVersion> findById(Long id) {
        RagVersionPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<RagVersion> findByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<RagVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionPO::getKnowledgeBaseId, knowledgeBaseId)
               .eq(RagVersionPO::getDeleted, 0)
               .orderByDesc(RagVersionPO::getPublishedAt);
        
        List<RagVersionPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RagVersion> findByKnowledgeBaseIdAndVersion(Long knowledgeBaseId, String version) {
        LambdaQueryWrapper<RagVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionPO::getKnowledgeBaseId, knowledgeBaseId)
               .eq(RagVersionPO::getVersion, version)
               .eq(RagVersionPO::getDeleted, 0);
        
        RagVersionPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<RagVersion> findPublished(int limit) {
        LambdaQueryWrapper<RagVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(RagVersionPO::getPublishedAt)
               .eq(RagVersionPO::getDeleted, 0)
               .orderByDesc(RagVersionPO::getPublishedAt)
               .last("LIMIT " + limit);
        
        List<RagVersionPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RagVersion update(RagVersion ragVersion) {
        RagVersionPO po = converter.toPO(ragVersion);
        mapper.updateById(po);
        return ragVersion;
    }

    @Override
    public boolean deleteById(Long id) {
        RagVersionPO po = new RagVersionPO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int countByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<RagVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionPO::getKnowledgeBaseId, knowledgeBaseId)
               .eq(RagVersionPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }
}
