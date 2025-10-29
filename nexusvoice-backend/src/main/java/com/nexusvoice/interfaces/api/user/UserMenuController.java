package com.nexusvoice.interfaces.api.user;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.rbac.MenuService;
import com.nexusvoice.application.rbac.assembler.SimpleMenuAssembler;
import com.nexusvoice.application.rbac.dto.MenuDTO;
import com.nexusvoice.application.rbac.dto.SimpleMenuDTO;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户菜单控制器
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "用户菜单", description = "用户菜单和权限查询接口")
@RestController
@RequestMapping("/api/user/menus")
@RequireAuth
public class UserMenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 查询当前用户的菜单树
     */
    @Operation(summary = "查询用户菜单树", description = "查询当前登录用户的菜单树（用于前端动态路由）")
    @GetMapping("/tree")
    public Result<List<MenuDTO>> getUserMenuTree() {
        // 从SecurityContext获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED));
        log.info("查询用户菜单树，userId：{}", userId);
        
        List<MenuDTO> menuTree = menuService.getUserMenuTree(userId);
        return Result.success("查询成功", menuTree);
    }

    /**
     * 查询当前用户的权限标识列表
     */
    @Operation(summary = "查询用户权限", description = "查询当前登录用户的权限标识列表（用于前端按钮权限控制）")
    @GetMapping("/permissions")
    public Result<List<String>> getUserPermissions() {
        // 从SecurityContext获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED));
        log.info("查询用户权限列表，userId：{}", userId);
        
        List<String> permissions = menuService.getUserPermissions(userId);
        return Result.success("查询成功", permissions);
    }

    /**
     * 查询当前用户的简化菜单树（前端兼容接口）
     * 返回适配前端AppRouteRecord结构的简化菜单
     */
    @Operation(summary = "查询简化菜单树", description = "查询当前登录用户的简化菜单树（适配前端路由结构，按钮聚合为authList）")
    @GetMapping("/simple")
    public Result<List<SimpleMenuDTO>> getSimpleMenuTree() {
        // 从SecurityContext获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED));
        log.info("查询用户简化菜单树，userId：{}", userId);
        
        // 获取菜单树
        List<MenuDTO> menuTree = menuService.getUserMenuTree(userId);
        
        // 转换为简化结构
        List<SimpleMenuDTO> simpleMenuTree = SimpleMenuAssembler.toSimpleMenuTree(menuTree);
        
        return Result.success("查询成功", simpleMenuTree);
    }
}
