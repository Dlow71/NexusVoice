package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * User持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Mapper
public interface UserPOMapper extends BaseMapper<UserPO> {
}
