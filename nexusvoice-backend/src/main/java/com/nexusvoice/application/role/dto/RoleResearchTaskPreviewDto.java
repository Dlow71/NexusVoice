package com.nexusvoice.application.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 深研任务清单预览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "深研任务清单预览")
public class RoleResearchTaskPreviewDto {
    @Schema(description = "建议的任务列表")
    private List<RoleResearchTaskDto> tasks;

    @Schema(description = "建议默认条目上限", example = "12")
    private Integer defaultLimit;

    @Schema(description = "系统最大允许条目上限", example = "20")
    private Integer maxLimit;
}

