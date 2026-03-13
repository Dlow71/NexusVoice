package com.nexusvoice.application.rag.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "知识库文件响应")
public class KnowledgeBaseFileResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "处理状态")
    private String status;

    @Schema(description = "处理进度")
    private BigDecimal processProgress;

    @Schema(description = "切分块数量")
    private Integer chunkCount;

    @Schema(description = "已向量化块数量")
    private Integer vectorizedChunkCount;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "完成时间")
    private LocalDateTime processedAt;
}
