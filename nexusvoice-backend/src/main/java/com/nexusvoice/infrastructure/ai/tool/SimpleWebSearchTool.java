package com.nexusvoice.infrastructure.ai.tool;

import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 简化版网页搜索工具
 * 供LangChain4j AI代理使用的搜索功能
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@Component
public class SimpleWebSearchTool {
    
    private final SearchRepository searchRepository;
    
    public SimpleWebSearchTool(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }
    
    /**
     * 搜索网页信息
     * 
     * @param query 搜索查询，应该是具体的问题或关键词
     * @return 搜索结果的文本摘要
     */
    @Tool("搜索网页获取最新信息。当需要查找实时信息、新闻、事实或者用户问的问题涉及你不知道的知识时使用此工具。")
    public String searchWeb(String query) {
        try {
            log.info("AI代理调用网页搜索，查询：{}", query);
            
            SearchResult result = searchRepository.searchWeb(query, 5, "zh-CN");
            
            if (result.getItems() == null || result.getItems().isEmpty()) {
                return "抱歉，没有找到关于\"" + query + "\"的相关信息。";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("搜索\"").append(query).append("\"找到了以下信息：\n\n");
            
            int count = 1;
            for (SearchResult.SearchItem item : result.getItems()) {
                if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                    summary.append(count).append(". **").append(item.getTitle()).append("**\n");
                }
                if (item.getSnippet() != null && !item.getSnippet().isEmpty()) {
                    summary.append(item.getSnippet());
                }
                if (item.getLink() != null && !item.getLink().isEmpty()) {
                    summary.append("\n来源：").append(item.getLink());
                }
                summary.append("\n\n");
                count++;
                
                // 限制返回内容长度避免token过多
                if (summary.length() > 2000) {
                    break;
                }
            }
            
            String finalResult = summary.toString();
            log.info("网页搜索完成，返回结果长度：{}", finalResult.length());
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("网页搜索工具执行异常", e);
            return "搜索过程中发生错误：" + e.getMessage();
        }
    }
}
