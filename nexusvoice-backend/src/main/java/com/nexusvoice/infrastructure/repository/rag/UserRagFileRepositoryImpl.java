package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.UserRagFile;
import com.nexusvoice.domain.rag.repository.UserRagFileRepository;
import com.nexusvoice.infrastructure.persistence.converter.UserRagFilePOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.UserRagFilePOMapper;
import com.nexusvoice.infrastructure.persistence.po.UserRagFilePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户RAG文件仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class UserRagFileRepositoryImpl implements UserRagFileRepository {

    private final UserRagFilePOMapper mapper;
    private final UserRagFilePOConverter converter;

    public UserRagFileRepositoryImpl(UserRagFilePOMapper mapper, UserRagFilePOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public UserRagFile save(UserRagFile userRagFile) {
        UserRagFilePO po = converter.toPO(userRagFile);
        if (userRagFile.getId() == null) {
            mapper.insert(po);
            userRagFile.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return userRagFile;
    }

    @Override
    public List<UserRagFile> saveAll(List<UserRagFile> files) {
        for (UserRagFile file : files) {
            save(file);
        }
        return files;
    }

    @Override
    public Optional<UserRagFile> findById(Long id) {
        UserRagFilePO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<UserRagFile> findByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagFilePO::getUserRagId, userRagId)
               .eq(UserRagFilePO::getDeleted, 0)
               .orderByAsc(UserRagFilePO::getFileName);
        
        List<UserRagFilePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRagFile> findByOriginalFileId(Long originalFileId) {
        LambdaQueryWrapper<UserRagFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagFilePO::getOriginalFileId, originalFileId)
               .eq(UserRagFilePO::getDeleted, 0)
               .orderByDesc(UserRagFilePO::getCreatedAt);
        
        List<UserRagFilePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public UserRagFile update(UserRagFile userRagFile) {
        UserRagFilePO po = converter.toPO(userRagFile);
        mapper.updateById(po);
        return userRagFile;
    }

    @Override
    public boolean deleteById(Long id) {
        // 逻辑删除
        UserRagFilePO po = new UserRagFilePO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int deleteByUserRagId(Long userRagId) {
        // 批量逻辑删除
        LambdaQueryWrapper<UserRagFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagFilePO::getUserRagId, userRagId)
               .eq(UserRagFilePO::getDeleted, 0);
        
        UserRagFilePO updatePo = new UserRagFilePO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int countByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagFilePO::getUserRagId, userRagId)
               .eq(UserRagFilePO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public Long sumFileSizeByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagFilePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagFilePO::getUserRagId, userRagId)
               .eq(UserRagFilePO::getDeleted, 0);
        
        List<UserRagFilePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .mapToLong(po -> po.getFileSize() != null ? po.getFileSize() : 0L)
                .sum();
    }
}
