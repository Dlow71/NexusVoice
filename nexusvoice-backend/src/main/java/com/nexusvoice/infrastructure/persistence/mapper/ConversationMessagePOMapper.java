package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.ConversationMessagePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ConversationMessage持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Mapper
public interface ConversationMessagePOMapper extends BaseMapper<ConversationMessagePO> {
}
