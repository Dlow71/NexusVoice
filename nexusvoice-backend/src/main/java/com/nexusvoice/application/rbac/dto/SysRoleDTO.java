package com.nexusvoice.application.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统角色DTO
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Data
@Schema(description = "系统角色DTO")
public class SysRoleDTO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色编码", example = "admin")
    private String roleCode;

    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-启用", example = "1")
    private Integer status;

    @Schema(description = "是否系统内置角色", example = "false")
    private Boolean isSystem;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
