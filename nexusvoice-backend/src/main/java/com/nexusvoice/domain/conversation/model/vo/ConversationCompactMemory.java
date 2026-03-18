package com.nexusvoice.domain.conversation.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话压缩记忆。
 * 用于保存已经从原始历史中提炼出的摘要，减少后续轮次上下文占用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCompactMemory {

    /**
     * 压缩摘要正文。
     */
    private String summary;

    /**
     * 当前摘要覆盖到的最后一条消息序号。
     */
    private Integer summaryUntilSequence;

    /**
     * 摘要来源消息数量。
     */
    private Integer sourceMessageCount;

    /**
     * 摘要自身的估算token数。
     */
    private Integer estimatedTokens;

    /**
     * 生成该摘要时所使用的模型。
     */
    private String modelKey;

    /**
     * 最后更新时间。
     */
    private LocalDateTime updatedAt;

    public boolean hasSummary() {
        return summary != null && !summary.isBlank();
    }
}
