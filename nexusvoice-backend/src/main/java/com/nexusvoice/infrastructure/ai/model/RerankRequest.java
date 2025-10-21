package com.nexusvoice.infrastructure.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 重排序请求
 *
 * @author NexusVoice
 * @since 2025-10-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RerankRequest {
    
    /**
     * 查询文本
     */
    private String query;
    
    /**
     * 待排序的文档列表
     */
    private List<String> documents;
    
    /**
     * 返回前N个结果（可选，默认返回全部）
     */
    private Integer topN;
    
    /**
     * 用户ID（用于日志记录）
     */
    private Long userId;
    
    /**
     * 业务标识（用于日志记录）
     */
    private String bizId;
    
    /**
     * 是否返回相关性分数（默认true）
     */
    private Boolean returnScore;
    
    /**
     * 获取实际的topN值
     */
    public Integer getActualTopN() {
        if (topN == null || topN <= 0) {
            return documents != null ? documents.size() : 10;
        }
        return Math.min(topN, documents != null ? documents.size() : topN);
    }
}
