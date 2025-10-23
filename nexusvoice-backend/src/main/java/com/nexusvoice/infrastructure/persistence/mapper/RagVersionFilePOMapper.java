package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.RagVersionFilePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * RagVersionFile持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-10-23
 */
@Mapper
public interface RagVersionFilePOMapper extends BaseMapper<RagVersionFilePO> {
}
