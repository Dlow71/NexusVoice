package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.UserRag;
import com.nexusvoice.domain.rag.model.enums.InstallType;
import com.nexusvoice.domain.rag.repository.UserRagRepository;
import com.nexusvoice.infrastructure.persistence.converter.UserRagPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.UserRagPOMapper;
import com.nexusvoice.infrastructure.persistence.po.UserRagPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户RAG仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class UserRagRepositoryImpl implements UserRagRepository {

    private final UserRagPOMapper mapper;
    private final UserRagPOConverter converter;

    public UserRagRepositoryImpl(UserRagPOMapper mapper, UserRagPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public UserRag save(UserRag userRag) {
        UserRagPO po = converter.toPO(userRag);
        if (userRag.getId() == null) {
            mapper.insert(po);
            userRag.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return userRag;
    }

    @Override
    public Optional<UserRag> findById(Long id) {
        UserRagPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<UserRag> findByUserId(Long userId) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getUserId, userId)
               .eq(UserRagPO::getDeleted, 0)
               .orderByDesc(UserRagPO::getUpdatedAt)
               .orderByDesc(UserRagPO::getCreatedAt);
        
        List<UserRagPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRag> findByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getRagVersionId, ragVersionId)
               .eq(UserRagPO::getDeleted, 0)
               .orderByDesc(UserRagPO::getCreatedAt);
        
        List<UserRagPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserRag> findByUserIdAndOriginalKnowledgeBaseId(Long userId, Long originalKnowledgeBaseId) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getUserId, userId)
               .eq(UserRagPO::getOriginalKnowledgeBaseId, originalKnowledgeBaseId)
               .eq(UserRagPO::getDeleted, 0);
        
        UserRagPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<UserRag> findByInstallType(InstallType installType, int limit) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getInstallType, installType.name())
               .eq(UserRagPO::getDeleted, 0)
               .orderByDesc(UserRagPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        List<UserRagPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public UserRag update(UserRag userRag) {
        UserRagPO po = converter.toPO(userRag);
        mapper.updateById(po);
        return userRag;
    }

    @Override
    public boolean deleteById(Long id) {
        UserRagPO po = new UserRagPO();
        po.setId(id);
        po.setDeleted(1);
        return mapper.updateById(po) > 0;
    }

    @Override
    public int countByUserId(Long userId) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getUserId, userId)
               .eq(UserRagPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public int countByRagVersionId(Long ragVersionId) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getRagVersionId, ragVersionId)
               .eq(UserRagPO::getDeleted, 0);
        return mapper.selectCount(wrapper).intValue();
    }

    @Override
    public boolean existsByUserIdAndRagVersionId(Long userId, Long ragVersionId) {
        LambdaQueryWrapper<UserRagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRagPO::getUserId, userId)
               .eq(UserRagPO::getRagVersionId, ragVersionId)
               .eq(UserRagPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper) > 0;
    }
}
