package com.nexusvoice.domain.rag.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG引用信息。
 * 用于把回答中的引用从“临时提示词编号”提升为结构化来源数据。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RagCitation {

    /**
     * 前端可直接使用的稳定来源ID。
     */
    private String id;

    /**
     * 用户可见标签，例如“来源1”。
     */
    private String label;

    /**
     * 系统提示词里的原始资料编号。
     */
    private Integer originalSourceIndex;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fileId;

    private String fileName;

    /**
     * 片段位置或范围，例如“12”或“12-13”。
     */
    private String location;

    /**
     * 命中的原文摘录。
     */
    private String snippet;

    private Double score;

    private List<String> matchedQueries;
}
