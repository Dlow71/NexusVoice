package com.nexusvoice.application.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话运行配置DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话运行配置")
public class ConversationRuntimeConfigDto {

    @Schema(description = "会话运行策略")
    private ConversationRuntimePolicyDto policy;

    @Schema(description = "上下文快照")
    private ConversationContextSnapshotDto contextSnapshot;
}
