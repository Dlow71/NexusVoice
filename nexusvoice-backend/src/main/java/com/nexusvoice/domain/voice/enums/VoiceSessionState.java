package com.nexusvoice.domain.voice.enums;

/**
 * 语音会话状态。
 */
public enum VoiceSessionState {

    PREPARING("准备中"),
    READY("待说话"),
    USER_SPEAKING("用户说话中"),
    UNDERSTANDING("理解中"),
    RETRIEVING("检索中"),
    REASONING("思考中"),
    RESPONDING_TEXT("文本生成中"),
    RESPONDING_AUDIO("语音播报中"),
    INTERRUPTING("打断中"),
    DEGRADED("降级中"),
    TERMINATED("已结束");

    private final String description;

    VoiceSessionState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
