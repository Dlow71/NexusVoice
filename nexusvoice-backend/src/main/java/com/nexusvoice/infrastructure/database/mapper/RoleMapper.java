package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.domain.role.model.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据访问接口
 *
 * @author NexusVoice
 * @since 2025-09-25
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
}
