package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import com.nexusvoice.utils.MarkdownTextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Markdown文本清理测试控制器
 * 用于测试Markdown文本清理功能
 *
 * @author NexusVoice Team
 * @since 2025-09-27
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/markdown")
@Tag(name = "Markdown测试", description = "Markdown文本清理功能测试接口")
public class MarkdownTestController {

    /**
     * 测试Markdown文本清理功能
     *
     * @param text 包含Markdown格式的文本
     * @return 清理结果和统计信息
     */
    @PostMapping("/clean")
    @Operation(summary = "清理Markdown文本", description = "清理Markdown格式符号，返回适合TTS的纯文本")
    public Result<Map<String, Object>> cleanMarkdown(
            @Parameter(description = "包含Markdown格式的原始文本", required = true)
            @RequestBody String text) {
        
        log.info("收到Markdown清理测试请求，原始文本长度：{}", text != null ? text.length() : 0);
        
        try {
            // 执行Markdown清理
            String cleanedText = MarkdownTextUtils.cleanForTTS(text);
            
            // 检查是否包含Markdown格式
            boolean containsMarkdown = MarkdownTextUtils.containsMarkdown(text);
            
            // 获取统计信息
            String stats = MarkdownTextUtils.getCleaningStats(text, cleanedText);
            
            // 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("originalText", text);
            result.put("cleanedText", cleanedText);
            result.put("containsMarkdown", containsMarkdown);
            result.put("stats", stats);
            result.put("originalLength", text != null ? text.length() : 0);
            result.put("cleanedLength", cleanedText != null ? cleanedText.length() : 0);
            
            log.info("Markdown清理完成：{}", stats);
            
            return Result.success("Markdown文本清理成功", result);
            
        } catch (Exception e) {
            log.error("Markdown清理失败：{}", e.getMessage(), e);
            return Result.error("Markdown文本清理失败：" + e.getMessage());
        }
    }

    /**
     * 测试常见Markdown格式的清理效果
     *
     * @return 预设测试用例的清理结果
     */
    @GetMapping("/demo")
    @Operation(summary = "Markdown清理演示", description = "展示常见Markdown格式的清理效果")
    public Result<Map<String, Object>> markdownDemo() {
        
        log.info("执行Markdown清理演示测试");
        
        // 预设测试用例
        String[] testCases = {
            "这是**加粗文本**和*斜体文本*的例子",
            "# 这是标题\n## 二级标题\n### 三级标题",
            "```java\npublic class Test {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}\n```",
            "这里有`行内代码`和[链接文本](https://example.com)",
            "- 列表项1\n- 列表项2\n- 列表项3",
            "1. 有序列表1\n2. 有序列表2\n3. 有序列表3",
            "> 这是引用文本\n> 多行引用",
            "![图片](https://example.com/image.png)",
            "~~删除线文本~~和**嵌套的*斜体加粗*文本**",
            "| 表格 | 列1 | 列2 |\n|------|-----|-----|\n| 行1  | 数据1 | 数据2 |"
        };
        
        Map<String, Object> results = new HashMap<>();
        
        for (int i = 0; i < testCases.length; i++) {
            String testCase = testCases[i];
            String cleaned = MarkdownTextUtils.cleanForTTS(testCase);
            String stats = MarkdownTextUtils.getCleaningStats(testCase, cleaned);
            
            Map<String, Object> caseResult = new HashMap<>();
            caseResult.put("original", testCase);
            caseResult.put("cleaned", cleaned);
            caseResult.put("stats", stats);
            
            results.put("testCase" + (i + 1), caseResult);
        }
        
        log.info("Markdown清理演示完成，共测试{}个用例", testCases.length);
        
        return Result.success("Markdown清理演示完成", results);
    }

    /**
     * 检查文本是否包含Markdown格式
     *
     * @param text 待检查的文本
     * @return 检查结果
     */
    @PostMapping("/check")
    @Operation(summary = "检查Markdown格式", description = "检查文本是否包含Markdown格式符号")
    public Result<Map<String, Object>> checkMarkdown(
            @Parameter(description = "待检查的文本", required = true)
            @RequestBody String text) {
        
        log.info("检查文本是否包含Markdown格式，文本长度：{}", text != null ? text.length() : 0);
        
        try {
            boolean containsMarkdown = MarkdownTextUtils.containsMarkdown(text);
            
            Map<String, Object> result = new HashMap<>();
            result.put("text", text);
            result.put("containsMarkdown", containsMarkdown);
            result.put("length", text != null ? text.length() : 0);
            
            log.info("Markdown格式检查完成，包含Markdown：{}", containsMarkdown);
            
            return Result.success("Markdown格式检查完成", result);
            
        } catch (Exception e) {
            log.error("Markdown格式检查失败：{}", e.getMessage(), e);
            return Result.error("Markdown格式检查失败：" + e.getMessage());
        }
    }
}
