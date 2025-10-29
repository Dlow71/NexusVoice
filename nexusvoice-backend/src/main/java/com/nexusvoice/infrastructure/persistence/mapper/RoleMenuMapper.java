package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.RoleMenuPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联Mapper接口
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenuPO> {
}
