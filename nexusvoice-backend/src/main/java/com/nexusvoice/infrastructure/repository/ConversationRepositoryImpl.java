package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.domain.conversation.constant.ConversationStatus;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.infrastructure.database.entity.ConversationEntity;
import com.nexusvoice.infrastructure.database.mapper.ConversationMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 对话仓储实现类
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationMapper conversationMapper;

    public ConversationRepositoryImpl(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationEntity entity = convertToEntity(conversation);
        
        if (entity.getId() == null) {
            // 新增
            conversationMapper.insert(entity);
        } else {
            // 更新
            conversationMapper.updateById(entity);
        }
        
        return convertToDomain(entity);
    }

    @Override
    public Optional<Conversation> findById(Long conversationId) {
        ConversationEntity entity = conversationMapper.selectById(conversationId);
        return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
    }

    @Override
    public Optional<Conversation> findByIdAndUserId(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationEntity::getId, conversationId)
                    .eq(ConversationEntity::getUserId, userId)
                    .eq(ConversationEntity::getDeleted, 0);
        
        ConversationEntity entity = conversationMapper.selectOne(queryWrapper);
        return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
    }

    @Override
    public List<Conversation> findByUserId(Long userId) {
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationEntity::getUserId, userId)
                    .eq(ConversationEntity::getDeleted, 0)
                    .orderByDesc(ConversationEntity::getLastActiveAt);
        
        List<ConversationEntity> entities = conversationMapper.selectList(queryWrapper);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findByUserIdAndStatus(Long userId, ConversationStatus status) {
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationEntity::getUserId, userId)
                    .eq(ConversationEntity::getStatus, status.name())
                    .eq(ConversationEntity::getDeleted, 0)
                    .orderByDesc(ConversationEntity::getLastActiveAt);
        
        List<ConversationEntity> entities = conversationMapper.selectList(queryWrapper);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findByUserIdWithPaging(Long userId, Integer page, Integer size) {
        Page<ConversationEntity> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationEntity::getUserId, userId)
                    .eq(ConversationEntity::getDeleted, 0)
                    .orderByDesc(ConversationEntity::getLastActiveAt);
        
        Page<ConversationEntity> result = conversationMapper.selectPage(pageRequest, queryWrapper);
        return result.getRecords().stream()
                     .map(this::convertToDomain)
                     .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findRecentByUserId(Long userId, Integer limit) {
        List<ConversationEntity> entities = conversationMapper.findRecentByUserId(userId, limit);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> searchByUserIdAndKeyword(Long userId, String keyword) {
        List<ConversationEntity> entities = conversationMapper.searchByUserIdAndKeyword(userId, keyword);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public Long countByUserId(Long userId) {
        LambdaQueryWrapper<ConversationEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationEntity::getUserId, userId)
                    .eq(ConversationEntity::getDeleted, 0);
        
        return conversationMapper.selectCount(queryWrapper);
    }

    @Override
    public Long countByUserIdAndStatus(Long userId, ConversationStatus status) {
        return conversationMapper.countByUserIdAndStatus(userId, status.name());
    }

    @Override
    public void deleteById(Long conversationId) {
        conversationMapper.deleteById(conversationId);
    }

    @Override
    public void logicalDeleteById(Long conversationId) {
        LambdaUpdateWrapper<ConversationEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationEntity::getId, conversationId)
                     .set(ConversationEntity::getDeleted, 1)
                     .set(ConversationEntity::getUpdatedAt, LocalDateTime.now());
        
        conversationMapper.update(null, updateWrapper);
    }

    @Override
    public void updateStatusByIds(List<Long> conversationIds, ConversationStatus status) {
        conversationMapper.updateStatusByIds(conversationIds, status.name());
    }

    @Override
    public void deleteArchivedConversationsBefore(LocalDateTime dateTime) {
        conversationMapper.deleteArchivedConversationsBefore(dateTime);
    }

    @Override
    public boolean existsById(Long conversationId) {
        return conversationMapper.selectById(conversationId) != null;
    }

    @Override
    public boolean existsByIdAndUserId(Long conversationId, Long userId) {
        return conversationMapper.existsByIdAndUserId(conversationId, userId);
    }

    /**
     * 将领域对象转换为数据库实体
     */
    private ConversationEntity convertToEntity(Conversation conversation) {
        ConversationEntity entity = new ConversationEntity();
        BeanUtils.copyProperties(conversation, entity);
        
        if (conversation.getStatus() != null) {
            entity.setStatus(conversation.getStatus().name());
        }
        
        return entity;
    }

    /**
     * 将数据库实体转换为领域对象
     */
    private Conversation convertToDomain(ConversationEntity entity) {
        Conversation conversation = new Conversation();
        BeanUtils.copyProperties(entity, conversation);
        
        if (entity.getStatus() != null) {
            conversation.setStatus(ConversationStatus.valueOf(entity.getStatus()));
        }
        
        return conversation;
    }
}
