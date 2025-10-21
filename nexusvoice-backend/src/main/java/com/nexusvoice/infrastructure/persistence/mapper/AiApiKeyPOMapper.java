package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.AiApiKeyPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AiApiKey持久化对象Mapper
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Mapper
public interface AiApiKeyPOMapper extends BaseMapper<AiApiKeyPO> {
}
