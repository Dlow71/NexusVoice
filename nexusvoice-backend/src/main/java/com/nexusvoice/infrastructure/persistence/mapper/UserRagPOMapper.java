package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.UserRagPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserRag持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-10-23
 */
@Mapper
public interface UserRagPOMapper extends BaseMapper<UserRagPO> {
}
