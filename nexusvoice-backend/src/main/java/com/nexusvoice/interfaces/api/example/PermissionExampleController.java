package com.nexusvoice.interfaces.api.example;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.annotation.RequirePermission;
import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 权限控制示例Controller
 * 演示如何使用@RequirePermission注解进行细粒度权限控制
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Slf4j
@Tag(name = "权限示例", description = "演示@RequirePermission注解的使用")
@RestController
@RequestMapping("/api/example/permission")
@RequireAuth
public class PermissionExampleController {

    /**
     * 示例1：单一权限控制
     * 只有拥有system:user:view权限的用户才能访问
     */
    @Operation(summary = "查看用户列表", description = "需要system:user:view权限")
    @GetMapping("/users")
    @RequirePermission("system:user:view")
    public Result<String> listUsers() {
        log.info("查看用户列表");
        return Result.success("用户列表数据");
    }

    /**
     * 示例2：AND关系 - 必须同时拥有多个权限
     * 需要同时拥有system:user:view和system:user:add权限
     */
    @Operation(summary = "添加用户", description = "需要system:user:view和system:user:add权限")
    @PostMapping("/users")
    @RequirePermission(value = {"system:user:view", "system:user:add"}, logical = RequirePermission.Logical.AND)
    public Result<String> addUser() {
        log.info("添加用户");
        return Result.success("用户添加成功");
    }

    /**
     * 示例3：OR关系 - 拥有任一权限即可
     * 拥有system:user:edit或system:user:delete任一权限即可访问
     */
    @Operation(summary = "修改或删除用户", description = "需要system:user:edit或system:user:delete权限")
    @PutMapping("/users/{id}")
    @RequirePermission(value = {"system:user:edit", "system:user:delete"}, logical = RequirePermission.Logical.OR)
    public Result<String> updateUser(@PathVariable Long id) {
        log.info("修改用户，userId：{}", id);
        return Result.success("用户修改成功");
    }

    /**
     * 示例4：类级别权限控制
     * 如果在类上添加@RequirePermission，则该类的所有方法都需要该权限
     * 方法级别的@RequirePermission会覆盖类级别的设置
     */
    @Operation(summary = "删除用户", description = "需要system:user:delete权限")
    @DeleteMapping("/users/{id}")
    @RequirePermission("system:user:delete")
    public Result<String> deleteUser(@PathVariable Long id) {
        log.info("删除用户，userId：{}", id);
        return Result.success("用户删除成功");
    }

    /**
     * 示例5：管理员自动拥有所有权限
     * 管理员用户会自动通过所有权限检查
     */
    @Operation(summary = "管理员操作", description = "需要system:admin:operation权限，管理员自动通过")
    @PostMapping("/admin/operation")
    @RequirePermission("system:admin:operation")
    public Result<String> adminOperation() {
        log.info("执行管理员操作");
        return Result.success("操作成功");
    }
}
