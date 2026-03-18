package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 语音会话持久化对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_sessions")
public class VoiceSessionPO extends BasePO {

    @TableField("voice_session_id")
    private String voiceSessionId;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;

    @TableField("state")
    private String state;

    @TableField("transport_mode")
    private String transportMode;

    @TableField("response_mode")
    private String responseMode;

    @TableField("selected_model")
    private String selectedModel;

    @TableField("selected_voice_type")
    private String selectedVoiceType;

    @TableField("selected_asr_model")
    private String selectedAsrModel;

    @TableField("knowledge_base_ids")
    private String knowledgeBaseIds;

    @TableField("strict_mode")
    private Boolean strictMode;

    @TableField("rag_enabled")
    private Boolean ragEnabled;

    @TableField("compact_enabled")
    private Boolean compactEnabled;

    @TableField("show_thinking")
    private Boolean showThinking;

    @TableField("thinking_mode")
    private String thinkingMode;

    @TableField("context_strategy")
    private String contextStrategy;

    @TableField("runtime_config_snapshot")
    private String runtimeConfigSnapshot;

    @TableField("client_capabilities")
    private String clientCapabilities;

    @TableField("session_summary")
    private String sessionSummary;

    @TableField("current_turn_no")
    private Integer currentTurnNo;

    @TableField("last_error_code")
    private String lastErrorCode;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;
}
