package com.nexusvoice.application.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 深研任务条目（预览/可编辑）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "深研任务条目")
public class RoleResearchTaskDto {
    @Schema(description = "任务ID(本次预览内唯一)")
    private String id;

    @Schema(description = "建议查询语句")
    private String query;

    @Schema(description = "任务理由/目标")
    private String rationale;

    @Schema(description = "是否启用该任务", defaultValue = "true")
    private Boolean enabled;
}

