package com.nexusvoice.interfaces.api.system;

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
 * 系统菜单控制器（前端兼容接口）
 * 提供 /api/system/menus/* 路径，与旧版前端兼容
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "系统菜单", description = "系统菜单查询接口（兼容旧版）")
@RestController
@RequestMapping("/api/system/menus")
@RequireAuth
public class SystemMenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 查询当前用户的简化菜单列表（前端兼容接口）
     * 返回适配前端AppRouteRecord结构的简化菜单
     * 
     * 此接口路径为 /api/system/menus/simple，与前端路由守卫调用保持一致
     */
    @Operation(summary = "查询简化菜单列表", description = "查询当前登录用户的简化菜单列表（适配前端路由结构）")
    @GetMapping("/simple")
    public Result<List<SimpleMenuDTO>> getSimpleMenuList() {
        // 从SecurityContext获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BizException(ErrorCodeEnum.UNAUTHORIZED));
        log.info("查询用户简化菜单列表，userId：{}", userId);
        
        // 获取菜单树
        List<MenuDTO> menuTree = menuService.getUserMenuTree(userId);
        
        // 转换为简化结构（扁平化+按钮聚合）
        List<SimpleMenuDTO> simpleMenuList = SimpleMenuAssembler.toSimpleMenuTree(menuTree);
        
        log.info("简化菜单查询成功，userId：{}，菜单数：{}", userId, simpleMenuList.size());
        return Result.success("查询成功", simpleMenuList);
    }
}
