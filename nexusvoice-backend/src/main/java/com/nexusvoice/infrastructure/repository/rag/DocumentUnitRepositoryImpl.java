package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.repository.DocumentUnitRepository;
import com.nexusvoice.infrastructure.persistence.converter.DocumentUnitPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.DocumentUnitPOMapper;
import com.nexusvoice.infrastructure.persistence.po.DocumentUnitPO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文档单元仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class DocumentUnitRepositoryImpl implements DocumentUnitRepository {

    private final DocumentUnitPOMapper mapper;
    private final DocumentUnitPOConverter converter;

    public DocumentUnitRepositoryImpl(DocumentUnitPOMapper mapper, DocumentUnitPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public DocumentUnit save(DocumentUnit documentUnit) {
        DocumentUnitPO po = converter.toPO(documentUnit);
        if (documentUnit.getId() == null) {
            mapper.insert(po);
            documentUnit.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return documentUnit;
    }

    @Override
    public List<DocumentUnit> saveAll(List<DocumentUnit> documentUnits) {
        for (DocumentUnit documentUnit : documentUnits) {
            save(documentUnit);
        }
        return documentUnits;
    }

    @Override
    public Optional<DocumentUnit> findById(Long id) {
        DocumentUnitPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<DocumentUnit> findByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getDeleted, 0)
               .orderByAsc(DocumentUnitPO::getPage);
        
        List<DocumentUnitPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentUnit> findByFileIdAndPageNumber(Long fileId, Integer pageNumber) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getPage, pageNumber)
               .eq(DocumentUnitPO::getDeleted, 0)
               .orderByAsc(DocumentUnitPO::getPage);
        
        List<DocumentUnitPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DocumentUnit> findByFileIdAndChunkIndex(Long fileId, Integer chunkIndex) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getPage, chunkIndex)
               .eq(DocumentUnitPO::getDeleted, 0)
               .last("LIMIT 1");
        
        DocumentUnitPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<DocumentUnit> findUnvectorized(int limit) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getIsVector, false)
               .eq(DocumentUnitPO::getDeleted, 0)
               .orderByAsc(DocumentUnitPO::getFileId)
               .orderByAsc(DocumentUnitPO::getPage)
               .last("LIMIT " + limit);
        
        List<DocumentUnitPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentUnit> findUnvectorizedByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getIsVector, false)
               .eq(DocumentUnitPO::getDeleted, 0)
               .orderByAsc(DocumentUnitPO::getPage);
        
        List<DocumentUnitPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentUnit update(DocumentUnit documentUnit) {
        return save(documentUnit);
    }

    @Override
    public void markAsVectorized(Long id) {
        DocumentUnitPO po = new DocumentUnitPO();
        po.setId(id);
        po.setIsVector(true);
        mapper.updateById(po);
    }

    @Override
    public void markAsVectorizedBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            markAsVectorized(id);
        }
    }

    @Override
    public int countByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public Long sumCharCountByFileId(Long fileId) {
        return findByFileId(fileId).stream()
                .map(DocumentUnit::getContent)
                .filter(content -> content != null)
                .mapToLong(String::length)
                .sum();
    }

    @Override
    public Long sumTokenCountByFileId(Long fileId) {
        return findByFileId(fileId).stream()
                .map(DocumentUnit::getContent)
                .filter(content -> content != null)
                .mapToLong(content -> (long) Math.ceil(content.length() / 3.0))
                .sum();
    }

    @Override
    public int countVectorizedByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getIsVector, true)
               .eq(DocumentUnitPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public List<DocumentUnit> findOcrUnitsByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getIsOcr, true)
               .eq(DocumentUnitPO::getDeleted, 0)
               .orderByAsc(DocumentUnitPO::getPage);
        
        List<DocumentUnitPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentUnit> findByFileIdAndLanguage(Long fileId, String language) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getDeleted, 0)
               .orderByAsc(DocumentUnitPO::getPage);
        
        List<DocumentUnitPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void updateOcrInfo(Long id, BigDecimal confidence) {
        DocumentUnitPO po = new DocumentUnitPO();
        po.setId(id);
        po.setIsOcr(true);
        mapper.updateById(po);
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public int deleteByFileId(Long fileId) {
        LambdaQueryWrapper<DocumentUnitPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentUnitPO::getFileId, fileId)
               .eq(DocumentUnitPO::getDeleted, 0);
        return mapper.delete(wrapper);
    }
}
