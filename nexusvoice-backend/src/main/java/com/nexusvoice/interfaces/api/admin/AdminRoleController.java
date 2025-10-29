package com.nexusvoice.interfaces.api.admin;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.application.rbac.SysRoleService;
import com.nexusvoice.application.rbac.dto.SysRoleDTO;
import com.nexusvoice.application.rbac.dto.request.RoleCreateRequest;
import com.nexusvoice.application.rbac.dto.request.RoleUpdateRequest;
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
 * 管理员-角色管理控制器
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "管理员-角色管理", description = "系统角色的增删改查、权限分配等管理接口")
@RestController
@RequestMapping("/api/admin/roles")
@RequireAdmin
public class AdminRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    /**
     * 创建角色
     */
    @Operation(summary = "创建角色", description = "创建新的系统角色并分配菜单权限")
    @PostMapping
    public Result<SysRoleDTO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        log.info("管理员创建角色，roleCode：{}", request.getRoleCode());
        SysRoleDTO role = sysRoleService.createRole(request);
        return Result.success("角色创建成功", role);
    }

    /**
     * 更新角色
     */
    @Operation(summary = "更新角色", description = "更新系统角色信息和菜单权限")
    @PutMapping("/{roleId}")
    public Result<SysRoleDTO> updateRole(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @Valid @RequestBody RoleUpdateRequest request) {
        log.info("管理员更新角色，roleId：{}", roleId);
        request.setId(roleId);
        SysRoleDTO role = sysRoleService.updateRole(request);
        return Result.success("角色更新成功", role);
    }

    /**
     * 删除角色
     */
    @Operation(summary = "删除角色", description = "删除系统角色（系统内置角色不允许删除）")
    @DeleteMapping("/{roleId}")
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        log.info("管理员删除角色，roleId：{}", roleId);
        sysRoleService.deleteRole(roleId);
        return Result.success("角色删除成功");
    }

    /**
     * 查询角色详情
     */
    @Operation(summary = "查询角色详情", description = "根据角色ID查询详细信息，包含菜单权限")
    @GetMapping("/{roleId}")
    public Result<SysRoleDTO> getRoleById(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        SysRoleDTO role = sysRoleService.getRoleById(roleId);
        return Result.success("查询成功", role);
    }

    /**
     * 查询所有角色
     */
    @Operation(summary = "查询所有角色", description = "查询系统中所有角色列表")
    @GetMapping
    public Result<List<SysRoleDTO>> listRoles() {
        List<SysRoleDTO> roles = sysRoleService.listRoles();
        return Result.success("查询成功", roles);
    }

    /**
     * 查询用户的角色列表
     */
    @Operation(summary = "查询用户角色", description = "查询指定用户拥有的角色列表")
    @GetMapping("/user/{userId}")
    public Result<List<SysRoleDTO>> listUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        List<SysRoleDTO> roles = sysRoleService.listUserRoles(userId);
        return Result.success("查询成功", roles);
    }

    /**
     * 为用户分配角色
     */
    @Operation(summary = "分配用户角色", description = "为指定用户批量分配角色")
    @PostMapping("/user/{userId}/assign")
    public Result<Void> assignRolesToUser(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "角色ID列表") @RequestBody List<Long> roleIds) {
        log.info("管理员为用户分配角色，userId：{}, roleIds：{}", userId, roleIds);
        sysRoleService.assignRolesToUser(userId, roleIds);
        return Result.success("角色分配成功");
    }
}
