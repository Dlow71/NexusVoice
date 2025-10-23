package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.RagVersionFile;
import com.nexusvoice.domain.rag.repository.RagVersionFileRepository;
import com.nexusvoice.infrastructure.persistence.converter.RagVersionFilePOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.RagVersionFilePOMapper;
import com.nexusvoice.infrastructure.persistence.po.RagVersionFilePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RAG版本文件仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class RagVersionFileRepositoryImpl implements RagVersionFileRepository {

    private final RagVersionFilePOMapper mapper;
    private final RagVersionFilePOConverter converter;

    public RagVersionFileRepositoryImpl(RagVersionFilePOMapper mapper, RagVersionFilePOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public RagVersionFile save(RagVersionFile ragVersionFile) {
        RagVersionFilePO po = converter.toPO(ragVersionFile);
        if (ragVersionFile.getId() == null) {
            mapper.insert(po);
            ragVersionFile.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return ragVersionFile;
    }

    @Override
    public List<RagVersionFile> saveAll(List<RagVersionFile> files) {
        for (RagVersionFile file : files) {
            save(file);
        }
        return files;
    }

    @Override
    public Optional<RagVersionFile> findById(Long id) {
        RagVersionFilePO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<RagVersionFile> findByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionFilePO::getRagVersionId, ragVersionId)
               .eq(RagVersionFilePO::getDeleted, 0)
               .orderByAsc(RagVersionFilePO::getFileName);
        
        List<RagVersionFilePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RagVersionFile> findByOriginalFileId(Long originalFileId) {
        LambdaQueryWrapper<RagVersionFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionFilePO::getOriginalFileId, originalFileId)
               .eq(RagVersionFilePO::getDeleted, 0)
               .orderByDesc(RagVersionFilePO::getCreatedAt);
        
        List<RagVersionFilePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public RagVersionFile update(RagVersionFile ragVersionFile) {
        return save(ragVersionFile);
    }

    @Override
    public boolean deleteById(Long id) {
        RagVersionFilePO po = new RagVersionFilePO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int deleteByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionFilePO::getRagVersionId, ragVersionId)
               .eq(RagVersionFilePO::getDeleted, 0);
        
        RagVersionFilePO updatePo = new RagVersionFilePO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int countByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionFilePO::getRagVersionId, ragVersionId)
               .eq(RagVersionFilePO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public Long sumFileSizeByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<RagVersionFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagVersionFilePO::getRagVersionId, ragVersionId)
               .eq(RagVersionFilePO::getDeleted, 0);
        
        List<RagVersionFilePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(RagVersionFilePO::getFileSize)
                .filter(size -> size != null)
                .mapToLong(Long::longValue)
                .sum();
    }
}
