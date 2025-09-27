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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DuckDuckGo搜索服务实现
 * 使用DuckDuckGo Instant Answer API（完全免费）
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "nexusvoice.search.provider", havingValue = "duckduckgo", matchIfMissing = true)
public class DuckDuckGoSearchRepositoryImpl implements SearchRepository {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${nexusvoice.search.duckduckgo.baseUrl:https://api.duckduckgo.com/}")
    private String baseUrl;
    
    public DuckDuckGoSearchRepositoryImpl(@Qualifier("searchRestTemplate") RestTemplate restTemplate, 
                                         ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public SearchResult searchWeb(String query, Integer maxResults, String language) {
        // 输入验证
        if (query == null || query.trim().isEmpty()) {
            log.warn("搜索查询为空");
            return createEmptyResult("", System.currentTimeMillis());
        }
        
        // 参数默认值设置
        int finalMaxResults = (maxResults != null && maxResults > 0) ? Math.min(maxResults, 20) : 5;
        String finalLanguage = (language != null && !language.trim().isEmpty()) ? language : "zh-CN";
        
        long startTime = System.currentTimeMillis();
        String trimmedQuery = query.trim();
        
        try {
            String encodedQuery = URLEncoder.encode(trimmedQuery, StandardCharsets.UTF_8);
            String url = baseUrl + "?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1";
            
            log.info("执行DuckDuckGo搜索，查询：{}，最大结果数：{}", trimmedQuery, finalMaxResults);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "NexusVoice/1.0 (https://nexusvoice.com)");
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseSearchResponse(response.getBody(), trimmedQuery, finalMaxResults, startTime);
            } else {
                log.error("DuckDuckGo搜索请求失败，状态码：{}，查询：{}", response.getStatusCode(), trimmedQuery);
                return createEmptyResult(trimmedQuery, startTime);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("搜索参数无效，查询：{}", trimmedQuery, e);
            return createEmptyResult(trimmedQuery, startTime);
        } catch (Exception e) {
            log.error("DuckDuckGo搜索异常，查询：{}", trimmedQuery, e);
            return createEmptyResult(trimmedQuery, startTime);
        }
    }
    
    @Override
    public SearchResult searchNews(String query, Integer maxResults, String language) {
        // DuckDuckGo API 不区分新闻搜索，使用相同的实现
        return searchWeb(query + " news", maxResults, language);
    }
    
    /**
     * 解析搜索响应
     */
    private SearchResult parseSearchResponse(String responseBody, String query, int maxResults, long startTime) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.warn("搜索响应为空");
            return createEmptyResult(query, startTime);
        }
        
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<SearchResult.SearchItem> items = new ArrayList<>();
            
            // 解析 Answer（直接答案优先级最高）
            parseAnswerSection(root, items);
            
            // 解析 Abstract（摘要）
            parseAbstractSection(root, items);
            
            // 解析 RelatedTopics（相关主题）
            parseRelatedTopicsSection(root, items, maxResults);
            
            return SearchResult.builder()
                    .query(query)
                    .items(items)
                    .searchTimeMs(System.currentTimeMillis() - startTime)
                    .totalCount(items.size())
                    .source("DuckDuckGo")
                    .build();
                    
        } catch (Exception e) {
            log.error("解析DuckDuckGo搜索结果异常，查询：{}", query, e);
            return createEmptyResult(query, startTime);
        }
    }
    
    /**
     * 解析直接答案部分
     */
    private void parseAnswerSection(JsonNode root, List<SearchResult.SearchItem> items) {
        if (root.has("Answer") && !root.get("Answer").asText().trim().isEmpty()) {
            String answerText = root.get("Answer").asText().trim();
            String answerUrl = root.has("AnswerURL") ? root.get("AnswerURL").asText().trim() : "";
            
            items.add(SearchResult.SearchItem.builder()
                    .title("直接答案")
                    .snippet(answerText)
                    .link(answerUrl)
                    .relevanceScore(1.1)
                    .build());
        }
    }
    
    /**
     * 解析摘要部分
     */
    private void parseAbstractSection(JsonNode root, List<SearchResult.SearchItem> items) {
        if (root.has("Abstract") && !root.get("Abstract").asText().trim().isEmpty()) {
            String abstractText = root.get("Abstract").asText().trim();
            String heading = root.has("Heading") ? root.get("Heading").asText().trim() : "摘要";
            String abstractUrl = root.has("AbstractURL") ? root.get("AbstractURL").asText().trim() : "";
            
            if (heading.isEmpty()) {
                heading = "摘要";
            }
            
            items.add(SearchResult.SearchItem.builder()
                    .title(heading)
                    .snippet(abstractText)
                    .link(abstractUrl)
                    .relevanceScore(1.0)
                    .build());
        }
    }
    
    /**
     * 解析相关主题部分
     */
    private void parseRelatedTopicsSection(JsonNode root, List<SearchResult.SearchItem> items, int maxResults) {
        if (!root.has("RelatedTopics") || !root.get("RelatedTopics").isArray()) {
            return;
        }
        
        JsonNode relatedTopics = root.get("RelatedTopics");
        int count = 0;
        // 预留位置给Answer和Abstract
        int remainingSlots = Math.max(1, maxResults - items.size());
        
        for (JsonNode topic : relatedTopics) {
            if (count >= remainingSlots) {
                break;
            }
            
            if (topic.has("Text") && topic.has("FirstURL")) {
                String text = topic.get("Text").asText().trim();
                String url = topic.get("FirstURL").asText().trim();
                
                if (text.isEmpty() || url.isEmpty()) {
                    continue;
                }
                
                // 提取标题（限制长度）
                String title = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                double relevanceScore = Math.max(0.1, 0.8 - (count * 0.1));
                
                items.add(SearchResult.SearchItem.builder()
                        .title(title)
                        .snippet(text)
                        .link(url)
                        .relevanceScore(relevanceScore)
                        .build());
                count++;
            }
        }
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
                .source("DuckDuckGo")
                .build();
    }
}
