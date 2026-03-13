package com.nexusvoice.application.rag.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "RAG检索结果")
public class RagSearchResultDto {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "文档块ID")
    private Long documentUnitId;

    @Schema(description = "文件ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    @Schema(description = "文件标题")
    private String title;

    @Schema(description = "分数")
    private Double score;

    @Schema(description = "内容")
    private String content;
}
