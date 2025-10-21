package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.AiApiCallLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AiApiCallLog持久化对象Mapper
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Mapper
public interface AiApiCallLogPOMapper extends BaseMapper<AiApiCallLogPO> {
}
