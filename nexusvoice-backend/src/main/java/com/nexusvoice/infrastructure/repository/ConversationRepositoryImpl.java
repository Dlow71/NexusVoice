package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusvoice.domain.conversation.constant.ConversationStatus;
import com.nexusvoice.domain.conversation.model.Conversation;
import com.nexusvoice.domain.conversation.repository.ConversationRepository;
import com.nexusvoice.infrastructure.persistence.converter.ConversationPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.ConversationPOMapper;
import com.nexusvoice.infrastructure.persistence.po.ConversationPO;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 对话仓储实现类
 * 使用PO层进行持久化操作
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Repository
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationPOMapper mapper;
    private final ConversationPOConverter converter;

    public ConversationRepositoryImpl(ConversationPOMapper mapper, ConversationPOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationPO po = converter.toPO(conversation);
        
        if (po.getId() == null) {
            mapper.insert(po);
            conversation.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        
        return converter.toDomain(po);
    }

    @Override
    public Optional<Conversation> findById(Long conversationId) {
        ConversationPO po = mapper.selectById(conversationId);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<Conversation> findByIdAndUserId(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getId, conversationId)
                    .eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0);
        
        ConversationPO po = mapper.selectOne(queryWrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<Conversation> findByUserId(Long userId) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0)
                    .orderByDesc(ConversationPO::getLastActiveAt);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findByUserIdAndStatus(Long userId, ConversationStatus status) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getStatus, convertStatusToString(status))
                    .eq(ConversationPO::getDeleted, 0)
                    .orderByDesc(ConversationPO::getLastActiveAt);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findByUserIdWithPaging(Long userId, Integer page, Integer size) {
        Page<ConversationPO> pageRequest = new Page<>(page, size);
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0)
                    .orderByDesc(ConversationPO::getLastActiveAt);
        
        Page<ConversationPO> result = mapper.selectPage(pageRequest, queryWrapper);
        return result.getRecords().stream()
                     .map(converter::toDomain)
                     .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> findRecentByUserId(Long userId, Integer limit) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0)
                    .orderByDesc(ConversationPO::getLastActiveAt)
                    .last("LIMIT " + limit);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public List<Conversation> searchByUserIdAndKeyword(Long userId, String keyword) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0)
                    .and(wrapper -> wrapper.like(ConversationPO::getTitle, keyword)
                                          .or()
                                          .like(ConversationPO::getSystemPrompt, keyword))
                    .orderByDesc(ConversationPO::getLastActiveAt);
        
        return mapper.selectList(queryWrapper).stream()
                      .map(converter::toDomain)
                      .collect(Collectors.toList());
    }

    @Override
    public Long countByUserId(Long userId) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0);
        
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public Long countByUserIdAndStatus(Long userId, ConversationStatus status) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getStatus, convertStatusToString(status))
                    .eq(ConversationPO::getDeleted, 0);
        
        return mapper.selectCount(queryWrapper);
    }

    @Override
    public void deleteById(Long conversationId) {
        mapper.deleteById(conversationId);
    }

    @Override
    public void logicalDeleteById(Long conversationId) {
        LambdaUpdateWrapper<ConversationPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ConversationPO::getId, conversationId)
                     .set(ConversationPO::getDeleted, 1)
                     .set(ConversationPO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, updateWrapper);
    }

    @Override
    public void updateStatusByIds(List<Long> conversationIds, ConversationStatus status) {
        LambdaUpdateWrapper<ConversationPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(ConversationPO::getId, conversationIds)
                     .set(ConversationPO::getStatus, convertStatusToString(status))
                     .set(ConversationPO::getUpdatedAt, LocalDateTime.now());
        
        mapper.update(null, updateWrapper);
    }

    @Override
    public void deleteArchivedConversationsBefore(LocalDateTime dateTime) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getStatus, "ARCHIVED")
                    .lt(ConversationPO::getUpdatedAt, dateTime);
        
        mapper.delete(queryWrapper);
    }

    @Override
    public boolean existsById(Long conversationId) {
        return mapper.selectById(conversationId) != null;
    }

    @Override
    public boolean existsByIdAndUserId(Long conversationId, Long userId) {
        LambdaQueryWrapper<ConversationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConversationPO::getId, conversationId)
                    .eq(ConversationPO::getUserId, userId)
                    .eq(ConversationPO::getDeleted, 0);
        
        return mapper.selectCount(queryWrapper) > 0;
    }

    /**
     * 状态枚举转String
     */
    private String convertStatusToString(ConversationStatus status) {
        if (status == null) {
            return "ACTIVE";
        }
        return status.name();
    }
}
