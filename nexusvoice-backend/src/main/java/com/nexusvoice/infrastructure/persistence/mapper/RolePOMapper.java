package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.RolePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Role持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Mapper
public interface RolePOMapper extends BaseMapper<RolePO> {
}
