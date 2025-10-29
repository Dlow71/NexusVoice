package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.UserRolePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联Mapper接口
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRolePO> {
}
