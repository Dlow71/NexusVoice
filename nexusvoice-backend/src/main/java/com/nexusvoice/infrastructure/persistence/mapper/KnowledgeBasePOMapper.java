package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.KnowledgeBasePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * KnowledgeBase持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-10-23
 */
@Mapper
public interface KnowledgeBasePOMapper extends BaseMapper<KnowledgeBasePO> {
}
