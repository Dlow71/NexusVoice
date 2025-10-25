package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.domain.conversation.model.ConversationMessage;
import com.nexusvoice.domain.conversation.repository.ConversationMessageRepository;
import com.nexusvoice.infrastructure.persistence.converter.ConversationMessagePOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.ConversationMessagePOMapper;
import com.nexusvoice.infrastructure.persistence.po.ConversationMessagePO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 对话消息仓储实现类
 * 使用PO层进行持久化操作
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Repository
public class ConversationMessageRepositoryImpl implements ConversationMessageRepository {

    private final ConversationMessagePOMapper mapper;
    private final ConversationMessagePOConverter converter;

    public ConversationMessageRepositoryImpl(ConversationMessagePOMapper mapper, 
                                            ConversationMessagePOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        ConversationMessagePO po = converter.toPO(message);
        
        if (po.getId() == null) {
            mapper.insert(po);
            message.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        
        return converter.toDomain(po);
    }

    @Override
    public List<ConversationMessage> saveAll(List<ConversationMessage> messages) {
        return messages.stream()
                      .map(this::save)
                      .collect(Collectors.toList());
    }

    @Override
    public Optional<ConversationMessage> findById(Long messageId) {
        ConversationMessagePO po = mapper.selectById(messageId);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<ConversationMessage> findByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0)
                    .orderByAsc(ConversationMessagePO::getSequence);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByConversationIdOrderBySequence(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0)
                    .orderByAsc(ConversationMessagePO::getSequence);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByConversationIdAndRole(Long conversationId, MessageRole role) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getRole, role.name().toLowerCase())
                    .eq(ConversationMessagePO::getDeleted, 0)
                    .orderByAsc(ConversationMessagePO::getSequence);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findByConversationIdWithPaging(Long conversationId, Integer page, Integer size) {
        Page<ConversationMessagePO> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0)
                    .orderByAsc(ConversationMessagePO::getSequence);
        
        Page<ConversationMessagePO> result = mapper.selectPage(pageRequest, queryWrapper);
        return result.getRecords().stream()
                     .map(converter::toDomain)
                     .collect(Collectors.toList());
    }

    @Override
    public List<ConversationMessage> findRecentByConversationId(Long conversationId, Integer limit) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0)
                    .orderByDesc(ConversationMessagePO::getSequence)
                    .last("LIMIT " + limit);
        
        List<ConversationMessagePO> messages = mapper.selectList(queryWrapper);
        // 按sequence正序返回
        return messages.stream()
                      .sorted((m1, m2) -> m1.getSequence().compareTo(m2.getSequence()))
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public Optional<ConversationMessage> findLastMessageByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0)
                    .orderByDesc(ConversationMessagePO::getSequence)
                    .last("LIMIT 1");
        
        ConversationMessagePO po = mapper.selectOne(queryWrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Integer getNextSequenceByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .orderByDesc(ConversationMessagePO::getSequence)
                    .last("LIMIT 1");
        
        ConversationMessagePO lastMessage = mapper.selectOne(queryWrapper);
        return lastMessage != null ? lastMessage.getSequence() + 1 : 1;
    }

    @Override
    public Long countByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0);
        
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public Long countByConversationIdAndRole(Long conversationId, MessageRole role) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getRole, role.name().toLowerCase())
                    .eq(ConversationMessagePO::getDeleted, 0);
        
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public Long sumTokenCountByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getDeleted, 0);
        
        List<ConversationMessagePO> messages = mapper.selectList(queryWrapper);
        return messages.stream()
                .map(ConversationMessagePO::getTokenCount)
                .filter(count -> count != null)
                .mapToLong(Integer::longValue)
                .sum();
    }

    @Override
    public void deleteById(Long messageId) {
        mapper.deleteById(messageId);
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId);
        
        mapper.delete(queryWrapper);
    }
    
    @Override
    public void logicalDeleteByConversationId(Long conversationId) {
        // 使用LambdaUpdateWrapper设置deleted字段，触发软删除
        LambdaUpdateWrapper<ConversationMessagePO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                     .set(ConversationMessagePO::getDeleted, 1)  // MyBatis-Plus软删除标识
                     .set(ConversationMessagePO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, updateWrapper);
    }

    @Override
    public void deleteByIds(List<Long> messageIds) {
        if (messageIds != null && !messageIds.isEmpty()) {
            LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(ConversationMessagePO::getId, messageIds);
            mapper.delete(queryWrapper);
        }
    }

    @Override
    public void updateContent(Long messageId, String content) {
        LambdaUpdateWrapper<ConversationMessagePO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationMessagePO::getId, messageId)
                     .set(ConversationMessagePO::getContent, content)
                     .set(ConversationMessagePO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, updateWrapper);
    }

    @Override
    public void updateStatus(Long messageId, String status) {
        LambdaUpdateWrapper<ConversationMessagePO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationMessagePO::getId, messageId)
                     .set(ConversationMessagePO::getStatus, status)
                     .set(ConversationMessagePO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, updateWrapper);
    }

    @Override
    public void updateTokenCount(Long messageId, Integer tokenCount) {
        LambdaUpdateWrapper<ConversationMessagePO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationMessagePO::getId, messageId)
                     .set(ConversationMessagePO::getTokenCount, tokenCount)
                     .set(ConversationMessagePO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, updateWrapper);
    }

    @Override
    public void updateStatusByIds(List<Long> messageIds, String status) {
        if (messageIds != null && !messageIds.isEmpty()) {
            LambdaUpdateWrapper<ConversationMessagePO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(ConversationMessagePO::getId, messageIds)
                         .set(ConversationMessagePO::getStatus, status)
                         .set(ConversationMessagePO::getUpdatedAt, LocalDateTime.now());
            
            mapper.update(null, updateWrapper);
        }
    }

    @Override
    public void deleteMessagesBefore(LocalDateTime dateTime) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(ConversationMessagePO::getCreatedAt, dateTime);
        
        mapper.delete(queryWrapper);
    }

    @Override
    public boolean existsById(Long messageId) {
        return mapper.selectById(messageId) != null;
    }

    @Override
    public boolean existsByConversationIdAndSequence(Long conversationId, Integer sequence) {
        LambdaQueryWrapper<ConversationMessagePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationMessagePO::getConversationId, conversationId)
                    .eq(ConversationMessagePO::getSequence, sequence)
                    .eq(ConversationMessagePO::getDeleted, 0);
        
        return mapper.selectCount(queryWrapper) > 0;
    }
}
