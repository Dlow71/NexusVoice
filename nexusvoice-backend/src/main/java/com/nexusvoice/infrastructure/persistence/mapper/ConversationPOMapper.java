package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.ConversationPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Conversation持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Mapper
public interface ConversationPOMapper extends BaseMapper<ConversationPO> {
}
