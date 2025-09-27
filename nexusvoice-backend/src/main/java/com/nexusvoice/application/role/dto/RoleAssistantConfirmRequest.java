package com.nexusvoice.application.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 确认创建角色请求
 */
@Data
@Schema(description = "确认创建角色请求")
public class RoleAssistantConfirmRequest {

    @Schema(description = "对话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "对话ID不能为空")
    private Long conversationId;

    @Schema(description = "是否启用深研模式", example = "false")
    private Boolean deepResearch = false;

    @Schema(description = "深研结果上限条数（可选）", example = "12")
    private Integer researchLimit;

    @Schema(description = "可选：覆盖草稿中的名称")
    private String overrideName;

    @Schema(description = "可选：覆盖草稿中的TTS声音（即将废弃，建议直接使用voiceType字段）")
    private String overrideVoiceType;

    @Schema(description = "可选：直接指定角色TTS音色（前端传参优先，例：qiniu_zh_female_dmytwz）")
    private String voiceType;

    @Schema(description = "可选：自定义深研查询集合（将替换系统建议）")
    private List<String> researchQueries;
}
