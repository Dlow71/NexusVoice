package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.SysRolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色Mapper接口
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRolePO> {

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Select("SELECT sr.* FROM sys_roles sr " +
            "INNER JOIN user_roles ur ON sr.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND sr.deleted = 0")
    List<SysRolePO> selectByUserId(@Param("userId") Long userId);
}
