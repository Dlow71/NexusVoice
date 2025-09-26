package com.nexusvoice.interfaces.api.role;

import com.nexusvoice.annotation.RequireUser;
import com.nexusvoice.application.role.dto.RoleCreateRequest;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleUpdateRequest;
import com.nexusvoice.application.role.service.RoleApplicationService;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端 - 角色浏览与私人角色管理
 */
@Tag(name = "角色", description = "公共角色浏览与私人角色管理")
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleApplicationService roleApplicationService;

    // ============== 公共角色浏览（无需登录） ==============

    @Operation(summary = "分页浏览公共角色", description = "分页浏览平台发布的公共角色，支持名称与描述搜索")
    @GetMapping("/public")
    public Result<PageResult<RoleDTO>> pagePublicRoles(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        PageResult<RoleDTO> result = roleApplicationService.pagePublicRoles(page, size, keyword);
        return Result.success("查询成功", result);
    }

    @Operation(summary = "公共角色详情", description = "查看公共角色详情")
    @GetMapping("/public/{id}")
    public Result<RoleDTO> getPublicRoleDetail(@PathVariable("id") Long id) {
        RoleDTO dto = roleApplicationService.getPublicRoleDetail(id);
        return Result.success("查询成功", dto);
    }

    // ============== 私人角色管理（需要登录用户） ==============

    @Operation(summary = "我的私人角色列表", description = "分页查看自己创建的私人角色")
    @RequireUser
    @GetMapping("/private")
    public Result<PageResult<RoleDTO>> pageMyPrivateRoles(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        PageResult<RoleDTO> result = roleApplicationService.pageMyPrivateRoles(userId, page, size, keyword);
        return Result.success("查询成功", result);
    }

    @Operation(summary = "创建私人角色", description = "创建仅自己可见的私人角色")
    @RequireUser
    @PostMapping("/private")
    public Result<RoleDTO> createPrivateRole(@Valid @RequestBody RoleCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        RoleDTO dto = roleApplicationService.createPrivateRole(userId, request);
        return Result.success("创建成功", dto);
    }

    @Operation(summary = "编辑私人角色", description = "编辑自己创建的私人角色")
    @RequireUser
    @PutMapping("/private/{id}")
    public Result<Void> updatePrivateRole(@PathVariable("id") Long id,
                                          @Valid @RequestBody RoleUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        roleApplicationService.updatePrivateRole(userId, id, request);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除私人角色", description = "删除自己创建的私人角色（逻辑删除）")
    @RequireUser
    @DeleteMapping("/private/{id}")
    public Result<Void> deletePrivateRole(@PathVariable("id") Long id) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录"));
        roleApplicationService.deletePrivateRole(userId, id);
        return Result.success("删除成功");
    }
}
