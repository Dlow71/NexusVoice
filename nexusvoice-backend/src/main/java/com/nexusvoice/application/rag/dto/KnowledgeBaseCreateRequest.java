package com.nexusvoice.application.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建知识库请求")
public class KnowledgeBaseCreateRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称不能超过100个字符")
    @Schema(description = "知识库名称", example = "产品资料库")
    private String name;

    @Size(max = 2000, message = "知识库描述不能超过2000个字符")
    @Schema(description = "知识库描述", example = "存放产品手册、FAQ、规范等资料")
    private String description;
}
