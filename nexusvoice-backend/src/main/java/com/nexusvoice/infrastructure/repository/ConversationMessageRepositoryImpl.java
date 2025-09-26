package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.infrastructure.database.entity.ConversationMessageEntity;
import com.nexusvoice.infrastructure.database.mapper.ConversationMessageMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 对话消息仓储实现类
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Repository
public class ConversationMessageRepositoryImpl implements ConversationMessageRepository {

    private final ConversationMessageMapper messageMapper;

    public ConversationMessageRepositoryImpl(ConversationMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        ConversationMessageEntity entity = convertToEntity(message);
        
        if (entity.getId() == null) {
            // 新增
            messageMapper.insert(entity);
        } else {
            // 更新
            messageMapper.updateById(entity);
        }
        
        return convertToDomain(entity);
    }

    @Override
    public List<ConversationMessage> saveAll(List<ConversationMessage> messages) {
        return messages.stream()
                      .map(this::save)
                      .collect(Collectors.toList());
    }

    @Override
    public Optional<ConversationMessage> findById(Long messageId) {
        ConversationMessageEntity entity = messageMapper.selectById(messageId);
        return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
    }

    @Override
    public List<ConversationMessage> findByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessageEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessageEntity::getConversationId, conversationId)
                    .eq(ConversationMessageEntity::getDeleted, 0)
                    .orderByAsc(ConversationMessageEntity::getSequence);
        
        List<ConversationMessageEntity> entities = messageMapper.selectList(queryWrapper);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByConversationIdOrderBySequence(Long conversationId) {
        List<ConversationMessageEntity> entities = messageMapper.findByConversationIdOrderBySequence(conversationId);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByConversationIdAndRole(Long conversationId, MessageRole role) {
        List<ConversationMessageEntity> entities = messageMapper.findByConversationIdAndRole(conversationId, role.name());
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByConversationIdWithPaging(Long conversationId, Integer page, Integer size) {
        Page<ConversationMessageEntity> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<ConversationMessageEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessageEntity::getConversationId, conversationId)
                    .eq(ConversationMessageEntity::getDeleted, 0)
                    .orderByAsc(ConversationMessageEntity::getSequence);
        
        Page<ConversationMessageEntity> result = messageMapper.selectPage(pageRequest, queryWrapper);
        return result.getRecords().stream()
                     .map(this::convertToDomain)
                     .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findRecentByConversationId(Long conversationId, Integer limit) {
        List<ConversationMessageEntity> entities = messageMapper.findRecentByConversationId(conversationId, limit);
        return entities.stream()
                      .map(this::convertToDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public Optional<ConversationMessage> findLastMessageByConversationId(Long conversationId) {
        ConversationMessageEntity entity = messageMapper.findLastMessageByConversationId(conversationId);
        return entity != null ? Optional.of(convertToDomain(entity)) : Optional.empty();
    }

    @Override
    public Integer getNextSequenceByConversationId(Long conversationId) {
        return messageMapper.getNextSequenceByConversationId(conversationId);
    }

    @Override
    public Long countByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessageEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessageEntity::getConversationId, conversationId)
                    .eq(ConversationMessageEntity::getDeleted, 0);
        
        return messageMapper.selectCount(queryWrapper);
    }

    @Override
    public Long countByConversationIdAndRole(Long conversationId, MessageRole role) {
        return messageMapper.countByConversationIdAndRole(conversationId, role.name());
    }

    @Override
    public Long sumTokenCountByConversationId(Long conversationId) {
        return messageMapper.sumTokenCountByConversationId(conversationId);
    }

    @Override
    public void deleteById(Long messageId) {
        messageMapper.deleteById(messageId);
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        messageMapper.deleteByConversationId(conversationId);
    }

    @Override
    public void deleteByIds(List<Long> messageIds) {
        messageMapper.deleteByIds(messageIds);
    }

    @Override
    public void updateContent(Long messageId, String content) {
        messageMapper.updateContent(messageId, content);
    }

    @Override
    public void updateStatus(Long messageId, String status) {
        messageMapper.updateStatus(messageId, status);
    }

    @Override
    public void updateTokenCount(Long messageId, Integer tokenCount) {
        messageMapper.updateTokenCount(messageId, tokenCount);
    }

    @Override
    public void updateStatusByIds(List<Long> messageIds, String status) {
        messageMapper.updateStatusByIds(messageIds, status);
    }

    @Override
    public void deleteMessagesBefore(LocalDateTime dateTime) {
        messageMapper.deleteMessagesBefore(dateTime);
    }

    @Override
    public boolean existsById(Long messageId) {
        return messageMapper.selectById(messageId) != null;
    }

    @Override
    public boolean existsByConversationIdAndSequence(Long conversationId, Integer sequence) {
        return messageMapper.existsByConversationIdAndSequence(conversationId, sequence);
    }

    /**
     * 将领域对象转换为数据库实体
     */
    private ConversationMessageEntity convertToEntity(ConversationMessage message) {
        ConversationMessageEntity entity = new ConversationMessageEntity();
        BeanUtils.copyProperties(message, entity);
        
        if (message.getRole() != null) {
            entity.setRole(message.getRole().name());
        }
        
        return entity;
    }

    /**
     * 将数据库实体转换为领域对象
     */
    private ConversationMessage convertToDomain(ConversationMessageEntity entity) {
        ConversationMessage message = new ConversationMessage();
        BeanUtils.copyProperties(entity, message);
        
        if (entity.getRole() != null) {
            message.setRole(MessageRole.valueOf(entity.getRole()));
        }
        
        return message;
    }
}
