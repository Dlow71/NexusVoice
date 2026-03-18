package com.nexusvoice.application.voice.dto;

import com.nexusvoice.application.conversation.dto.ConversationContextSnapshotDto;
import com.nexusvoice.domain.rag.model.vo.RagCitation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文本轮次处理结果。
 */
@Data
@Builder
public class VoiceTurnResultDto {

    private Integer turnNo;

    private String userTranscript;

    private String displayText;

    private String spokenText;

    private String reasoningContent;

    private List<RagCitation> citations;

    private List<AudioSegmentDto> audioSegments;

    private ConversationContextSnapshotDto contextSnapshot;

    @Data
    @Builder
    public static class AudioSegmentDto {
        private Integer segmentIndex;
        private String displayText;
        private String spokenText;
        private String audioUrl;
        private Boolean isLast;
    }
}
