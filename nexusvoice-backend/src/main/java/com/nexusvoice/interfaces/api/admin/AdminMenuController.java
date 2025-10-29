package com.nexusvoice.interfaces.api.admin;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.application.rbac.MenuService;
import com.nexusvoice.application.rbac.dto.MenuDTO;
import com.nexusvoice.application.rbac.dto.request.MenuSaveRequest;
import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-菜单管理控制器
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "管理员-菜单管理", description = "系统菜单的增删改查、树形结构查询等管理接口")
@RestController
@RequestMapping("/api/admin/menus")
@RequireAdmin
public class AdminMenuController {

    @Autowired
    private MenuService menuService;

    /**
     * 保存菜单（新增或更新）
     */
    @Operation(summary = "保存菜单", description = "创建新菜单或更新现有菜单（有ID则更新，无ID则新增）")
    @PostMapping
    public Result<MenuDTO> saveMenu(@Valid @RequestBody MenuSaveRequest request) {
        if (request.getId() != null) {
            log.info("管理员更新菜单，menuId：{}", request.getId());
        } else {
            log.info("管理员创建菜单，name：{}", request.getName());
        }
        MenuDTO menu = menuService.saveMenu(request);
        return Result.success("菜单保存成功", menu);
    }

    /**
     * 删除菜单
     */
    @Operation(summary = "删除菜单", description = "删除指定菜单（有子菜单的不允许删除）")
    @DeleteMapping("/{menuId}")
    public Result<Void> deleteMenu(@Parameter(description = "菜单ID") @PathVariable Long menuId) {
        log.info("管理员删除菜单，menuId：{}", menuId);
        menuService.deleteMenu(menuId);
        return Result.success("菜单删除成功");
    }

    /**
     * 查询菜单详情
     */
    @Operation(summary = "查询菜单详情", description = "根据菜单ID查询详细信息")
    @GetMapping("/{menuId}")
    public Result<MenuDTO> getMenuById(@Parameter(description = "菜单ID") @PathVariable Long menuId) {
        MenuDTO menu = menuService.getMenuById(menuId);
        return Result.success("查询成功", menu);
    }

    /**
     * 查询所有菜单（扁平列表）
     */
    @Operation(summary = "查询所有菜单", description = "查询系统中所有菜单的扁平列表")
    @GetMapping("/list")
    public Result<List<MenuDTO>> listMenus() {
        List<MenuDTO> menus = menuService.listMenus();
        return Result.success("查询成功", menus);
    }

    /**
     * 查询菜单树
     */
    @Operation(summary = "查询菜单树", description = "查询系统所有启用的菜单树形结构")
    @GetMapping("/tree")
    public Result<List<MenuDTO>> getMenuTree() {
        List<MenuDTO> menuTree = menuService.getMenuTree();
        return Result.success("查询成功", menuTree);
    }

    /**
     * 查询角色的菜单列表
     */
    @Operation(summary = "查询角色菜单", description = "查询指定角色拥有的菜单列表")
    @GetMapping("/role/{roleId}")
    public Result<List<MenuDTO>> getRoleMenus(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<MenuDTO> menus = menuService.getRoleMenus(roleId);
        return Result.success("查询成功", menus);
    }
}
