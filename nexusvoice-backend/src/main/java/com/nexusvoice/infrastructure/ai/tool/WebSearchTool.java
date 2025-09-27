package com.nexusvoice.infrastructure.ai.tool;

import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网页搜索工具
 * 供LangChain4j AI代理使用的搜索功能
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@Component
public class WebSearchTool {
    
    private final SearchRepository searchRepository;
    
    public WebSearchTool(SearchRepository searchRepository) {
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
            
            if (result.getItems().isEmpty()) {
                return "抱歉，没有找到关于\"" + query + "\"的相关信息。";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("搜索\"").append(query).append("\"找到了以下信息：\n\n");
            
            int count = 1;
            for (SearchResult.SearchItem item : result.getItems()) {
                summary.append(count).append(". **").append(item.getTitle()).append("**\n");
                summary.append(item.getSnippet());
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
            
            summary.append("(搜索耗时：").append(result.getSearchTimeMs()).append("ms)");
            
            String finalResult = summary.toString();
            log.info("网页搜索完成，返回结果长度：{}", finalResult.length());
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("网页搜索工具执行异常", e);
            return "搜索过程中发生错误，请稍后重试。";
        }
    }
    
    /**
     * 搜索新闻信息
     * 
     * @param query 新闻搜索查询
     * @return 新闻搜索结果摘要
     */
    @Tool("搜索最新新闻信息。当用户询问时事新闻、最新动态或特定事件的最新消息时使用。")
    public String searchNews(String query) {
        try {
            log.info("AI代理调用新闻搜索，查询：{}", query);
            
            SearchResult result = searchRepository.searchNews(query, 5, "zh-CN");
            
            if (result.getItems().isEmpty()) {
                return "抱歉，没有找到关于\"" + query + "\"的相关新闻。";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("搜索\"").append(query).append("\"的新闻找到以下信息：\n\n");
            
            int count = 1;
            for (SearchResult.SearchItem item : result.getItems()) {
                summary.append(count).append(". **").append(item.getTitle()).append("**\n");
                summary.append(item.getSnippet());
                if (item.getPublishTime() != null) {
                    summary.append("\n发布时间：").append(item.getPublishTime());
                }
                if (item.getLink() != null && !item.getLink().isEmpty()) {
                    summary.append("\n来源：").append(item.getLink());
                }
                summary.append("\n\n");
                count++;
                
                if (summary.length() > 2000) {
                    break;
                }
            }
            
            String finalResult = summary.toString();
            log.info("新闻搜索完成，返回结果长度：{}", finalResult.length());
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("新闻搜索工具执行异常", e);
            return "新闻搜索过程中发生错误，请稍后重试。";
        }
    }
}
