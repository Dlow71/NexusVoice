package com.nexusvoice.application.rag.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG引用原文上下文")
public class RagCitationContextDto {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "知识库ID")
    private Long knowledgeBaseId;

    @Schema(description = "知识库名称")
    private String knowledgeBaseName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "请求位置")
    private String requestedLocation;

    @Schema(description = "实际命中范围")
    private String resolvedLocation;

    @Schema(description = "上下文片段列表")
    private List<ContextSegmentDto> segments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "上下文片段")
    public static class ContextSegmentDto {
        @Schema(description = "片段页码")
        private Integer page;

        @Schema(description = "是否命中引用片段")
        private Boolean hit;

        @Schema(description = "片段内容")
        private String content;
    }
}
