package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.RagVersionDocument;
import com.nexusvoice.domain.rag.repository.RagVersionDocumentRepository;
import com.nexusvoice.infrastructure.persistence.converter.RagVersionDocumentPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.RagVersionDocumentPOMapper;
import com.nexusvoice.infrastructure.persistence.po.RagVersionDocumentPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RAG版本文档仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class RagVersionDocumentRepositoryImpl implements RagVersionDocumentRepository {

    private final RagVersionDocumentPOMapper mapper;
    private final RagVersionDocumentPOConverter converter;

    public RagVersionDocumentRepositoryImpl(RagVersionDocumentPOMapper mapper, RagVersionDocumentPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public RagVersionDocument save(RagVersionDocument ragVersionDocument) {
        RagVersionDocumentPO po = converter.toPO(ragVersionDocument);
        if (ragVersionDocument.getId() == null) {
            mapper.insert(po);
            ragVersionDocument.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return ragVersionDocument;
    }

    @Override
    public List<RagVersionDocument> saveAll(List<RagVersionDocument> documents) {
        for (RagVersionDocument document : documents) {
            save(document);
        }
        return documents;
    }

    @Override
    public Optional<RagVersionDocument> findById(Long id) {
        RagVersionDocumentPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<RagVersionDocument> findByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionId, ragVersionId)
               .eq(RagVersionDocumentPO::getDeleted, 0)
               .orderByAsc(RagVersionDocumentPO::getRagVersionFileId)
               .orderByAsc(RagVersionDocumentPO::getPage);
        
        List<RagVersionDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RagVersionDocument> findByRagVersionFileId(Long ragVersionFileId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionFileId, ragVersionFileId)
               .eq(RagVersionDocumentPO::getDeleted, 0)
               .orderByAsc(RagVersionDocumentPO::getPage);
        
        List<RagVersionDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RagVersionDocument> findByOriginalDocumentId(Long originalDocumentId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getOriginalDocumentId, originalDocumentId)
               .eq(RagVersionDocumentPO::getDeleted, 0)
               .orderByDesc(RagVersionDocumentPO::getCreatedAt);
        
        List<RagVersionDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RagVersionDocument> findUnvectorized(Long ragVersionId, int limit) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionId, ragVersionId)
               .and(w -> w.isNull(RagVersionDocumentPO::getVectorId)
                       .or()
                       .eq(RagVersionDocumentPO::getVectorId, ""))
               .eq(RagVersionDocumentPO::getDeleted, 0)
               .orderByAsc(RagVersionDocumentPO::getRagVersionFileId)
               .orderByAsc(RagVersionDocumentPO::getPage)
               .last("LIMIT " + limit);
        
        List<RagVersionDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RagVersionDocument update(RagVersionDocument ragVersionDocument) {
        return save(ragVersionDocument);
    }

    @Override
    public boolean deleteById(Long id) {
        RagVersionDocumentPO po = new RagVersionDocumentPO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int deleteByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionId, ragVersionId)
               .eq(RagVersionDocumentPO::getDeleted, 0);
        
        RagVersionDocumentPO updatePo = new RagVersionDocumentPO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int deleteByRagVersionFileId(Long ragVersionFileId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionFileId, ragVersionFileId)
               .eq(RagVersionDocumentPO::getDeleted, 0);
        
        RagVersionDocumentPO updatePo = new RagVersionDocumentPO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int countByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionId, ragVersionId)
               .eq(RagVersionDocumentPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public int countVectorizedByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionDocumentPO::getRagVersionId, ragVersionId)
               .isNotNull(RagVersionDocumentPO::getVectorId)
               .ne(RagVersionDocumentPO::getVectorId, "")
               .eq(RagVersionDocumentPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }
}
