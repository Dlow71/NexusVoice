package com.nexusvoice.infrastructure.agent.tool;

import com.nexusvoice.domain.agent.model.ToolParameter;
import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 搜索工具适配器
 * 
 * 职责：
 * - 将现有的SearchRepository适配为BaseTool接口
 * - 提供联网搜索能力
 */
@Slf4j
@Component
public class SearchToolAdapter implements BaseTool {
    
    @Autowired
    private SearchRepository searchRepository;
    
    @Override
    public String getName() {
        return "web_search";
    }
    
    @Override
    public String getDescription() {
        return "搜索互联网信息。适用于需要获取最新信息、查找资料、了解实时动态的场景。" +
               "输入搜索关键词，返回相关网页的标题、摘要和链接。";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            ToolParameter.builder()
                .name("query")
                .type("string")
                .description("搜索关键词")
                .required(true)
                .example("人工智能发展趋势")
                .build(),
            ToolParameter.builder()
                .name("limit")
                .type("number")
                .description("返回结果数量")
                .required(false)
                .defaultValue(10)
                .minValue(1.0)
                .maxValue(20.0)
                .example("10")
                .build(),
            ToolParameter.builder()
                .name("language")
                .type("string")
                .description("搜索语言")
                .required(false)
                .defaultValue("zh-CN")
                .enumValues(new String[]{"zh-CN", "en-US", "ja-JP"})
                .example("zh-CN")
                .build()
        );
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            // 提取参数
            String query = (String) parameters.get("query");
            if (query == null || query.trim().isEmpty()) {
                throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "搜索关键词不能为空");
            }
            
            Integer limit = parameters.containsKey("limit") 
                ? ((Number) parameters.get("limit")).intValue() 
                : 10;
            
            String language = (String) parameters.getOrDefault("language", "zh-CN");
            
            // 执行搜索
            log.info("执行搜索，关键词：{}，数量：{}，语言：{}", query, limit, language);
            SearchResult result = searchRepository.searchWeb(query, limit, language);
            
            // 返回JSON格式结果
            java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
            resultMap.put("success", true);
            resultMap.put("query", query);
            resultMap.put("items", result.getItems() != null ? result.getItems() : List.of());
            resultMap.put("count", result.getItems() != null ? result.getItems().size() : 0);
            return JsonUtils.toJson(resultMap);
            
        } catch (BizException e) {
            log.error("搜索工具执行失败", e);
            throw e;
        } catch (Exception e) {
            log.error("搜索工具执行异常", e);
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "搜索失败：" + e.getMessage());
        }
    }
    
    @Override
    public String getCategory() {
        return "information_retrieval";
    }
    
    @Override
    public Long getEstimatedDurationMs() {
        return 3000L;
    }
    
    @Override
    public Integer getPriority() {
        return 5;  // 较高优先级
    }
}

