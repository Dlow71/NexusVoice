package com.nexusvoice.application.rbac.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色更新请求
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Data
@Schema(description = "角色更新请求")
public class RoleUpdateRequest {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    @Schema(description = "角色名称", example = "运营人员")
    private String roleName;

    @Size(max = 200, message = "角色描述长度不能超过200")
    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-启用", example = "1")
    private Integer status;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}
