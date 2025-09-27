package com.nexusvoice.interfaces.api.role;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.application.role.dto.RoleCreateRequest;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleUpdateRequest;
import com.nexusvoice.application.role.service.RoleApplicationService;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 公共角色与用户私人角色审核管理
 *
 * 提供：
 * - 公共角色的创建、查询、编辑、删除
 * - 查看所有用户创建的私人角色
 */
@Tag(name = "管理端-角色管理", description = "公共角色管理与私人角色审核")
@RestController
@RequestMapping("/api/admin/roles")
@RequireAdmin
public class AdminRoleController {

    private static final Logger log = LoggerFactory.getLogger(AdminRoleController.class);

    @Autowired
    private RoleApplicationService roleApplicationService;

    // ===================== 公共角色管理 =====================

    @Operation(summary = "创建公共角色", description = "管理员创建平台级公共角色")
    @PostMapping
    public Result<RoleDTO> createPublicRole(@Valid @RequestBody RoleCreateRequest request) {
        log.info("管理员创建公共角色: name={}", request.getName());
        RoleDTO dto = roleApplicationService.createPublicRole(request);
        return Result.success("创建成功", dto);
    }

    @Operation(summary = "分页查询公共角色", description = "管理员分页查询与搜索公共角色")
    @GetMapping("/public")
    public Result<PageResult<RoleDTO>> pagePublicRoles(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword) {
        PageResult<RoleDTO> result = roleApplicationService.pagePublicRoles(page, size, keyword);
        return Result.success("查询成功", result);
    }

    @Operation(summary = "公共角色详情", description = "获取公共角色详情")
    @GetMapping("/public/{id}")
    public Result<RoleDTO> getPublicRoleDetail(@PathVariable("id") Long id) {
        RoleDTO dto = roleApplicationService.getPublicRoleDetail(id);
        return Result.success("查询成功", dto);
    }

    @Operation(summary = "编辑公共角色", description = "管理员编辑公共角色")
    @PutMapping("/public/{id}")
    public Result<Void> updatePublicRole(@PathVariable("id") Long id,
                                         @Valid @RequestBody RoleUpdateRequest request) {
        roleApplicationService.updatePublicRole(id, request);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除公共角色", description = "管理员删除公共角色（逻辑删除）")
    @DeleteMapping("/public/{id}")
    public Result<Void> deletePublicRole(@PathVariable("id") Long id) {
        roleApplicationService.deletePublicRole(id);
        return Result.success("删除成功");
    }

    // ===================== 用户私人角色审核 =====================

    @Operation(summary = "查看所有私人角色", description = "管理员分页查看所有用户创建的私人角色，可按用户过滤")
    @GetMapping("/private")
    public Result<PageResult<RoleDTO>> pageAllPrivateRoles(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户ID过滤") @RequestParam(required = false) Long userId) {
        PageResult<RoleDTO> result = roleApplicationService.pageAllPrivateRoles(page, size, keyword, userId);
        return Result.success("查询成功", result);
    }
}
