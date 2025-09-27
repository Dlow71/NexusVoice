package com.nexusvoice.infrastructure.tool.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tavily搜索服务实现
 * 使用Tavily Search API（专为AI优化的搜索引擎）
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "nexusvoice.search.provider", havingValue = "tavily")
public class TavilySearchRepositoryImpl implements SearchRepository {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${nexusvoice.search.tavily.base-url:https://api.tavily.com}")
    private String baseUrl;
    
    @Value("${nexusvoice.search.tavily.api-key}")
    private String apiKey;
    
    @Value("${nexusvoice.search.tavily.max-results:5}")
    private Integer defaultMaxResults;
    
    @Value("${nexusvoice.search.tavily.search-depth:basic}")
    private String defaultSearchDepth;
    
    @Value("${nexusvoice.search.tavily.include-answer:true}")
    private Boolean defaultIncludeAnswer;
    
    @Value("${nexusvoice.search.tavily.include-raw-content:false}")
    private Boolean defaultIncludeRawContent;
    
    @Value("${nexusvoice.search.tavily.include-images:false}")
    private Boolean defaultIncludeImages;
    
    public TavilySearchRepositoryImpl(@Qualifier("searchRestTemplate") RestTemplate restTemplate, 
                                     ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public SearchResult searchWeb(String query, Integer maxResults, String language) {
        return performSearch(query, maxResults, language, "general");
    }
    
    @Override
    public SearchResult searchNews(String query, Integer maxResults, String language) {
        return performSearch(query, maxResults, language, "news");
    }
    
    /**
     * 执行搜索请求
     */
    private SearchResult performSearch(String query, Integer maxResults, String language, String topic) {
        // 输入验证
        if (query == null || query.trim().isEmpty()) {
            log.warn("搜索查询为空");
            return createEmptyResult("", System.currentTimeMillis());
        }
        
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("please-set-in-application-local-yml")) {
            log.error("Tavily API密钥未配置，请在application-local.yml中设置nexusvoice.search.tavily.api-key");
            return createEmptyResult(query, System.currentTimeMillis());
        }
        
        // 参数默认值设置
        int finalMaxResults = (maxResults != null && maxResults > 0) ? Math.min(maxResults, 20) : defaultMaxResults;
        String trimmedQuery = query.trim();
        
        long startTime = System.currentTimeMillis();
        
        try {
            String url = baseUrl + "/search";
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", trimmedQuery);
            requestBody.put("search_depth", defaultSearchDepth);
            requestBody.put("include_answer", defaultIncludeAnswer);
            requestBody.put("include_raw_content", defaultIncludeRawContent);
            requestBody.put("include_images", defaultIncludeImages);
            requestBody.put("max_results", finalMaxResults);
            requestBody.put("topic", topic);
            
            // 如果是中文查询，设置国家为中国以获得更相关的结果
            if (containsChinese(trimmedQuery) && "general".equals(topic)) {
                requestBody.put("country", "china");
            }
            
            log.info("执行Tavily搜索，查询：{}，主题：{}，最大结果数：{}", trimmedQuery, topic, finalMaxResults);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseSearchResponse(response.getBody(), trimmedQuery, startTime);
            } else {
                log.error("Tavily搜索请求失败，状态码：{}，查询：{}", response.getStatusCode(), trimmedQuery);
                return createEmptyResult(trimmedQuery, startTime);
            }
            
        } catch (Exception e) {
            log.error("Tavily搜索异常，查询：{}", trimmedQuery, e);
            return createEmptyResult(trimmedQuery, startTime);
        }
    }
    
    /**
     * 解析搜索响应
     */
    private SearchResult parseSearchResponse(String responseBody, String query, long startTime) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.warn("Tavily搜索响应为空");
            return createEmptyResult(query, startTime);
        }
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<SearchResult.SearchItem> items = new ArrayList<>();
            
            // 解析answer（如果存在）
            if (root.has("answer") && !root.get("answer").asText().trim().isEmpty()) {
                String answer = root.get("answer").asText().trim();
                items.add(SearchResult.SearchItem.builder()
                        .title("AI生成答案")
                        .snippet(answer)
                        .link("")
                        .relevanceScore(1.2)
                        .build());
            }
            
            // 解析搜索结果
            if (root.has("results") && root.get("results").isArray()) {
                JsonNode results = root.get("results");
                for (int i = 0; i < results.size(); i++) {
                    JsonNode result = results.get(i);
                    
                    String title = result.has("title") ? result.get("title").asText() : "";
                    String content = result.has("content") ? result.get("content").asText() : "";
                    String url = result.has("url") ? result.get("url").asText() : "";
                    double score = result.has("score") ? result.get("score").asDouble() : 0.8 - (i * 0.1);
                    
                    if (!title.isEmpty() && !url.isEmpty()) {
                        items.add(SearchResult.SearchItem.builder()
                                .title(title)
                                .snippet(content.isEmpty() ? title : content)
                                .link(url)
                                .relevanceScore(Math.max(0.1, score))
                                .build());
                    }
                }
            }
            
            return SearchResult.builder()
                    .query(query)
                    .items(items)
                    .searchTimeMs(System.currentTimeMillis() - startTime)
                    .totalCount(items.size())
                    .source("Tavily")
                    .build();
                    
        } catch (Exception e) {
            log.error("解析Tavily搜索结果异常，查询：{}", query, e);
            return createEmptyResult(query, startTime);
        }
    }
    
    /**
     * 检查字符串是否包含中文字符
     */
    private boolean containsChinese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.chars().anyMatch(ch -> ch >= 0x4E00 && ch <= 0x9FFF);
    }
    
    /**
     * 创建空结果
     */
    private SearchResult createEmptyResult(String query, long startTime) {
        return SearchResult.builder()
                .query(query)
                .items(new ArrayList<>())
                .searchTimeMs(System.currentTimeMillis() - startTime)
                .totalCount(0)
                .source("Tavily")
                .build();
    }
}
