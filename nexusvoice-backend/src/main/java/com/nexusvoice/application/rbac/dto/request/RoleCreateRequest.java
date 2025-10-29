package com.nexusvoice.application.rbac.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色创建请求
 * 
 * @author NexusVoice
 * @since 2025-10-28
 */
@Data
@Schema(description = "角色创建请求")
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[a-z0-9_]{3,50}$", message = "角色编码格式不正确：只能包含小写字母、数字、下划线，长度3-50")
    @Schema(description = "角色编码", example = "operator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    @Schema(description = "角色名称", example = "运营人员", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleName;

    @Size(max = 200, message = "角色描述长度不能超过200")
    @Schema(description = "角色描述", example = "负责AI模型配置、AI角色管理等运营工作")
    private String description;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用 1-启用", example = "1")
    private Integer status;

    @Schema(description = "菜单ID列表")
    private List<Long> menuIds;
}
