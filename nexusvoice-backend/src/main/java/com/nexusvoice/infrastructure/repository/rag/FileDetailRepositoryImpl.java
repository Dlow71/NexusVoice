package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;
import com.nexusvoice.domain.rag.repository.FileDetailRepository;
import com.nexusvoice.infrastructure.persistence.converter.FileDetailPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.FileDetailPOMapper;
import com.nexusvoice.infrastructure.persistence.po.FileDetailPO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件详情仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class FileDetailRepositoryImpl implements FileDetailRepository {

    private final FileDetailPOMapper mapper;
    private final FileDetailPOConverter converter;

    public FileDetailRepositoryImpl(FileDetailPOMapper mapper, FileDetailPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public FileDetail save(FileDetail fileDetail) {
        FileDetailPO po = converter.toPO(fileDetail);
        if (fileDetail.getId() == null) {
            mapper.insert(po);
            fileDetail.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return fileDetail;
    }

    @Override
    public Optional<FileDetail> findById(Long id) {
        FileDetailPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<FileDetail> findByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getKnowledgeBaseId, knowledgeBaseId)
               .eq(FileDetailPO::getDeleted, 0)
               .orderByDesc(FileDetailPO::getCreatedAt);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetail> findByUserId(Long userId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getUserId, userId)
               .eq(FileDetailPO::getDeleted, 0)
               .orderByDesc(FileDetailPO::getCreatedAt);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetail> findByStatus(ProcessStatus status) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getStatus, status.name())
               .eq(FileDetailPO::getDeleted, 0)
               .orderByDesc(FileDetailPO::getCreatedAt);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FileDetail> findByFileHash(String fileHash) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getFileHash, fileHash)
               .eq(FileDetailPO::getDeleted, 0);
        
        FileDetailPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public FileDetail update(FileDetail fileDetail) {
        FileDetailPO po = converter.toPO(fileDetail);
        mapper.updateById(po);
        return fileDetail;
    }

    @Override
    public boolean deleteById(Long id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FileDetailPO::getId, ids)
               .eq(FileDetailPO::getDeleted, 0);
        return mapper.delete(wrapper);
    }

    @Override
    public void updateStatus(Long id, ProcessStatus status) {
        FileDetailPO po = new FileDetailPO();
        po.setId(id);
        po.setStatus(status.name());
        mapper.updateById(po);
    }

    @Override
    public void updateProgress(Long id, Integer currentPage, BigDecimal progress) {
        FileDetailPO po = new FileDetailPO();
        po.setId(id);
        po.setCurrentProcessPage(currentPage);
        po.setProcessProgress(progress);
        mapper.updateById(po);
    }

    @Override
    public void updateError(Long id, String errorCode, String errorMessage) {
        FileDetailPO po = new FileDetailPO();
        po.setId(id);
        po.setErrorCode(errorCode);
        po.setErrorMessage(errorMessage);
        mapper.updateById(po);
    }

    @Override
    public int countByUserId(Long userId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getUserId, userId)
               .eq(FileDetailPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public int countByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getKnowledgeBaseId, knowledgeBaseId)
               .eq(FileDetailPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public Long sumFileSizeByUserId(Long userId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getUserId, userId)
               .eq(FileDetailPO::getDeleted, 0);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .mapToLong(po -> po.getFileSize() != null ? po.getFileSize() : 0L)
                .sum();
    }

    @Override
    public Long sumFileSizeByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getKnowledgeBaseId, knowledgeBaseId)
               .eq(FileDetailPO::getDeleted, 0);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .mapToLong(po -> po.getFileSize() != null ? po.getFileSize() : 0L)
                .sum();
    }

    @Override
    public List<FileDetail> findPendingFiles(int limit) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getStatus, ProcessStatus.PENDING.name())
               .eq(FileDetailPO::getDeleted, 0)
               .orderByAsc(FileDetailPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetail> findFailedFilesByUserId(Long userId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getUserId, userId)
               .eq(FileDetailPO::getStatus, ProcessStatus.FAILED.name())
               .eq(FileDetailPO::getDeleted, 0)
               .orderByDesc(FileDetailPO::getCreatedAt);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileDetail> findByCreatedBefore(LocalDateTime beforeTime) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(FileDetailPO::getCreatedAt, beforeTime)
               .eq(FileDetailPO::getDeleted, 0)
               .orderByDesc(FileDetailPO::getCreatedAt);
        
        List<FileDetailPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean belongsToUser(Long fileId, Long userId) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getId, fileId)
               .eq(FileDetailPO::getUserId, userId)
               .eq(FileDetailPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public int countByUserIdAndFileType(Long userId, FileType fileType) {
        LambdaQueryWrapper<FileDetailPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileDetailPO::getUserId, userId)
               .eq(FileDetailPO::getFileType, fileType.name())
               .eq(FileDetailPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }
}
