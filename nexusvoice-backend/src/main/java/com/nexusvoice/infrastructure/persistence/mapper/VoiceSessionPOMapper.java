package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.VoiceSessionPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 语音会话Mapper。
 */
@Mapper
public interface VoiceSessionPOMapper extends BaseMapper<VoiceSessionPO> {
}
