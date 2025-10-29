package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.MenuPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper接口
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuPO> {

    /**
     * 根据角色ID查询菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    @Select("SELECT m.* FROM menus m " +
            "INNER JOIN role_menus rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort_order ASC")
    List<MenuPO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID列表查询菜单列表（去重）
     *
     * @param roleIds 角色ID列表
     * @return 菜单列表
     */
    @Select("<script>" +
            "SELECT DISTINCT m.* FROM menus m " +
            "INNER JOIN role_menus rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id IN " +
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach> " +
            "AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort_order ASC" +
            "</script>")
    List<MenuPO> selectByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 根据用户ID查询菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Select("SELECT DISTINCT m.* FROM menus m " +
            "INNER JOIN role_menus rm ON m.id = rm.menu_id " +
            "INNER JOIN user_roles ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort_order ASC")
    List<MenuPO> selectByUserId(@Param("userId") Long userId);
}
