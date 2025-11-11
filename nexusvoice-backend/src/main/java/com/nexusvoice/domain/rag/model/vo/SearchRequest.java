package com.nexusvoice.domain.rag.model.vo;

import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;
import lombok.Getter;

import java.util.List;

/**
 * 检索请求值对象
 * 
 * @author NexusVoice
 * @since 2025-11-11
 */
@Getter
public class SearchRequest {
    
    private final List<Long> knowledgeBaseIds;
    private final String question;
    private final Integer maxResults;
    private final Double minScore;
    private final boolean enableHyde;
    private final boolean enableRerank;
    
    /**
     * 私有构造函数，使用Builder模式
     */
    private SearchRequest(Builder builder) {
        this.knowledgeBaseIds = builder.knowledgeBaseIds;
        this.question = builder.question;
        this.maxResults = builder.maxResults;
        this.minScore = builder.minScore;
        this.enableHyde = builder.enableHyde;
        this.enableRerank = builder.enableRerank;
    }
    
    /**
     * 验证请求合法性
     */
    public void validate() {
        if (question == null || question.trim().isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "检索问题不能为空");
        }
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "知识库ID列表不能为空");
        }
        if (maxResults != null && maxResults <= 0) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "最大结果数必须大于0");
        }
        if (minScore != null && (minScore < 0 || minScore > 1)) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "最小分数必须在0-1之间");
        }
    }
    
    /**
     * Builder类
     */
    public static class Builder {
        private List<Long> knowledgeBaseIds;
        private String question;
        private Integer maxResults = 15;
        private Double minScore = 0.7;
        private boolean enableHyde = true;
        private boolean enableRerank = true;
        
        public Builder knowledgeBaseIds(List<Long> ids) {
            this.knowledgeBaseIds = ids;
            return this;
        }
        
        public Builder question(String question) {
            this.question = question;
            return this;
        }
        
        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        public Builder minScore(Double minScore) {
            this.minScore = minScore;
            return this;
        }
        
        public Builder enableHyde(boolean enable) {
            this.enableHyde = enable;
            return this;
        }
        
        public Builder enableRerank(boolean enable) {
            this.enableRerank = enable;
            return this;
        }
        
        public SearchRequest build() {
            return new SearchRequest(this);
        }
    }
}
