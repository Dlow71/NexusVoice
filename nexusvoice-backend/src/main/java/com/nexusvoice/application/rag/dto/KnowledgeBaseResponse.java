package com.nexusvoice.application.rag.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "知识库响应")
public class KnowledgeBaseResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "知识库ID")
    private Long id;

    @Schema(description = "知识库名称")
    private String name;

    @Schema(description = "知识库描述")
    private String description;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "文件数量")
    private Integer fileCount;

    @Schema(description = "总大小")
    private Long totalSize;

    @Schema(description = "文档块数量")
    private Integer documentCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "文件列表")
    private List<KnowledgeBaseFileResponse> files;
}
