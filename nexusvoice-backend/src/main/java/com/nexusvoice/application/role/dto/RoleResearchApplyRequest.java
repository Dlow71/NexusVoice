package com.nexusvoice.application.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 仅应用深研任务并更新草稿的请求
 */
@Data
@Schema(description = "应用深研任务并更新草稿请求")
public class RoleResearchApplyRequest {

    @Schema(description = "对话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    @Schema(description = "可选：自定义深研查询集合（为空则使用系统建议）")
    private List<String> researchQueries;

    @Schema(description = "深研结果上限条数（可选）", example = "12")
    private Integer researchLimit;
}

