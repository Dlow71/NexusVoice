package com.nexusvoice.infrastructure.ai.tool;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

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
    private final Cache<String, String> cache;
    private final boolean cacheEnabled;
    private final int maxResultsDefault;
    private final String defaultLanguage;
    
    public SimpleWebSearchTool(SearchRepository searchRepository,
                               @Value("${nexusvoice.search.cache.enabled:true}") boolean cacheEnabled,
                               @Value("${nexusvoice.search.cache.ttl-seconds:300}") long cacheTtlSeconds,
                               @Value("${nexusvoice.search.cache.max-size:1000}") long cacheMaxSize,
                               @Value("${nexusvoice.search.default-max-results:5}") int maxResultsDefault,
                               @Value("${nexusvoice.search.default-language:zh-CN}") String defaultLanguage) {
        this.searchRepository = searchRepository;
        this.cacheEnabled = cacheEnabled;
        this.maxResultsDefault = Math.max(1, Math.min(maxResultsDefault, 20));
        this.defaultLanguage = defaultLanguage != null ? defaultLanguage : "zh-CN";
        if (cacheEnabled) {
            this.cache = Caffeine.newBuilder()
                    .maximumSize(Math.max(100, cacheMaxSize))
                    .expireAfterWrite(Duration.ofSeconds(Math.max(60, cacheTtlSeconds)))
                    .build();
            log.info("SimpleWebSearchTool 缓存已启用，大小={}，TTL={}s", cacheMaxSize, cacheTtlSeconds);
        } else {
            this.cache = null;
            log.info("SimpleWebSearchTool 缓存未启用");
        }
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
            String norm = normalize(query);
            if (cacheEnabled && cache != null) {
                String cached = cache.getIfPresent(norm);
                if (cached != null) {
                    log.info("网页搜索命中缓存，query={}，length={}", norm, cached.length());
                    return cached;
                }
            }

            SearchResult result = searchRepository.searchWeb(query, maxResultsDefault, defaultLanguage);
            
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
            if (cacheEnabled && cache != null) {
                cache.put(norm, finalResult);
            }
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("网页搜索工具执行异常", e);
            return "搜索过程中发生错误：" + e.getMessage();
        }
    }

    private String normalize(String q) {
        if (q == null) return "";
        String t = q.trim().toLowerCase(Locale.ROOT);
        // 常见全角空白与标点标准化（简化）
        t = t.replace('，', ',').replace('。', '.').replace('：', ':').replace('；', ';');
        return t;
    }
}
