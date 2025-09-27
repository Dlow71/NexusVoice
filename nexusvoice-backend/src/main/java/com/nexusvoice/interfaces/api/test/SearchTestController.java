package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import com.nexusvoice.domain.tool.model.SearchResult;
import com.nexusvoice.domain.tool.repository.SearchRepository;
import com.nexusvoice.infrastructure.ai.tool.SimpleWebSearchTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索功能测试控制器
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/search")
@Tag(name = "搜索测试", description = "搜索功能测试接口")
public class SearchTestController {
    
    private final SearchRepository searchRepository;
    private final SimpleWebSearchTool searchTool;
    
    public SearchTestController(SearchRepository searchRepository, SimpleWebSearchTool searchTool) {
        this.searchRepository = searchRepository;
        this.searchTool = searchTool;
    }
    
    @GetMapping("/web")
    @Operation(summary = "测试网页搜索", description = "测试Tavily网页搜索功能")
    public Result<SearchResult> testWebSearch(
            @Parameter(description = "搜索查询", example = "人工智能最新发展")
            @RequestParam String query) {
        
        log.info("测试网页搜索，查询：{}", query);
        
        try {
            SearchResult result = searchRepository.searchWeb(query, 5, "zh-CN");
            return Result.success(result);
        } catch (Exception e) {
            log.error("网页搜索测试失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/tool")
    @Operation(summary = "测试搜索工具", description = "测试AI搜索工具功能")
    public Result<String> testSearchTool(
            @Parameter(description = "搜索查询", example = "今天的天气如何")
            @RequestParam String query) {
        
        log.info("测试搜索工具，查询：{}", query);
        
        try {
            String result = searchTool.searchWeb(query);
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索工具测试失败", e);
            return Result.error("搜索工具失败：" + e.getMessage());
        }
    }
}
