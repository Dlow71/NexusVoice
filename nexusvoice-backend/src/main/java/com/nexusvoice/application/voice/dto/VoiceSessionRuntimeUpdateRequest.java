package com.nexusvoice.application.voice.dto;

import com.nexusvoice.application.conversation.dto.ConversationRuntimePolicyDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 更新语音会话运行配置请求。
 */
@Data
@Schema(description = "更新语音会话运行配置请求")
public class VoiceSessionRuntimeUpdateRequest {

    @Schema(description = "严格模式")
    private Boolean strictMode;

    @Schema(description = "是否启用RAG")
    private Boolean ragEnabled;

    @Schema(description = "语音类型")
    private String voiceType;

    @Schema(description = "ASR模型键")
    private String asrModelKey;

    @Schema(description = "知识库ID列表")
    private List<Long> knowledgeBaseIds;

    @Schema(description = "运行策略")
    private ConversationRuntimePolicyDto policy;
}
