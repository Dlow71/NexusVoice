package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.RtcSessionPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * RTC会话Mapper
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Mapper
public interface RtcSessionPOMapper extends BaseMapper<RtcSessionPO> {
}

