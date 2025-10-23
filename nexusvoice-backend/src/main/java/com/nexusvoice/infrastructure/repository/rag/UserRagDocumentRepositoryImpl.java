package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.UserRagDocument;
import com.nexusvoice.domain.rag.repository.UserRagDocumentRepository;
import com.nexusvoice.infrastructure.persistence.converter.UserRagDocumentPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.UserRagDocumentPOMapper;
import com.nexusvoice.infrastructure.persistence.po.UserRagDocumentPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户RAG文档仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class UserRagDocumentRepositoryImpl implements UserRagDocumentRepository {

    private final UserRagDocumentPOMapper mapper;
    private final UserRagDocumentPOConverter converter;

    public UserRagDocumentRepositoryImpl(UserRagDocumentPOMapper mapper, UserRagDocumentPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public UserRagDocument save(UserRagDocument userRagDocument) {
        UserRagDocumentPO po = converter.toPO(userRagDocument);
        if (userRagDocument.getId() == null) {
            mapper.insert(po);
            userRagDocument.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return userRagDocument;
    }

    @Override
    public List<UserRagDocument> saveAll(List<UserRagDocument> documents) {
        for (UserRagDocument document : documents) {
            save(document);
        }
        return documents;
    }

    @Override
    public Optional<UserRagDocument> findById(Long id) {
        UserRagDocumentPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<UserRagDocument> findByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagId, userRagId)
               .eq(UserRagDocumentPO::getDeleted, 0)
               .orderByAsc(UserRagDocumentPO::getUserRagFileId)
               .orderByAsc(UserRagDocumentPO::getPage);
        
        List<UserRagDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRagDocument> findByUserRagFileId(Long userRagFileId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagFileId, userRagFileId)
               .eq(UserRagDocumentPO::getDeleted, 0)
               .orderByAsc(UserRagDocumentPO::getPage);
        
        List<UserRagDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRagDocument> findByOriginalDocumentId(Long originalDocumentId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getOriginalDocumentId, originalDocumentId)
               .eq(UserRagDocumentPO::getDeleted, 0)
               .orderByDesc(UserRagDocumentPO::getCreatedAt);
        
        List<UserRagDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRagDocument> findUnvectorized(Long userRagId, int limit) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagId, userRagId)
               .isNull(UserRagDocumentPO::getVectorId)
               .eq(UserRagDocumentPO::getDeleted, 0)
               .orderByAsc(UserRagDocumentPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        List<UserRagDocumentPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public UserRagDocument update(UserRagDocument userRagDocument) {
        UserRagDocumentPO po = converter.toPO(userRagDocument);
        mapper.updateById(po);
        return userRagDocument;
    }

    @Override
    public boolean deleteById(Long id) {
        UserRagDocumentPO po = new UserRagDocumentPO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int deleteByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagId, userRagId)
               .eq(UserRagDocumentPO::getDeleted, 0);
        
        UserRagDocumentPO updatePo = new UserRagDocumentPO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int deleteByUserRagFileId(Long userRagFileId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagFileId, userRagFileId)
               .eq(UserRagDocumentPO::getDeleted, 0);
        
        UserRagDocumentPO updatePo = new UserRagDocumentPO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public int countByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagId, userRagId)
               .eq(UserRagDocumentPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public int countVectorizedByUserRagId(Long userRagId) {
        LambdaQueryWrapper<UserRagDocumentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagDocumentPO::getUserRagId, userRagId)
               .isNotNull(UserRagDocumentPO::getVectorId)
               .eq(UserRagDocumentPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }
}
