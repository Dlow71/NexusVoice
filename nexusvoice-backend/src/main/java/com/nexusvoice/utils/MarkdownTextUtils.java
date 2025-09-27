package com.nexusvoice.utils;

import lombok.extern.slf4j.Slf4j;

/**
 * Markdown文本处理工具类
 * 用于清理Markdown格式符号，使文本适合TTS语音合成
 *
 * @author NexusVoice Team
 * @since 2025-09-27
 */
@Slf4j
public class MarkdownTextUtils {

    /**
     * 清理Markdown文本，移除格式符号，保留纯文本内容
     * 专门用于TTS语音合成前的文本预处理
     * 
     * @param markdownText 包含Markdown格式的原始文本
     * @return 清理后的纯文本，适合语音合成
     */
    public static String cleanForTTS(String markdownText) {
        if (markdownText == null || markdownText.trim().isEmpty()) {
            return "";
        }

        log.debug("开始清理Markdown文本，原始长度：{}", markdownText.length());
        
        String cleanedText = markdownText
            // 1. 移除代码块（```代码```）-> "代码块"
            .replaceAll("```[\\s\\S]*?```", "代码块")
            
            // 2. 移除行内代码（`代码`）-> 保留内容
            .replaceAll("`([^`]+)`", "$1")
            
            // 3. 移除加粗格式（**文本** 或 __文本__）-> 保留文本
            .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
            .replaceAll("__([^_]+)__", "$1")
            
            // 4. 移除斜体格式（*文本* 或 _文本_）-> 保留文本
            .replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "$1")
            .replaceAll("(?<!_)_([^_]+)_(?!_)", "$1")
            
            // 5. 移除删除线格式（~~文本~~）-> 保留文本
            .replaceAll("~~([^~]+)~~", "$1")
            
            // 6. 移除链接格式（[文本](链接)）-> 保留文本
            .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
            
            // 7. 移除图片格式（![alt](链接)）-> "图片"
            .replaceAll("!\\[[^\\]]*\\]\\([^)]+\\)", "图片")
            
            // 8. 移除标题符号（# ## ### 等）
            .replaceAll("(?m)^#{1,6}\\s+", "")
            
            // 9. 移除引用符号（> 引用内容）
            .replaceAll("(?m)^>+\\s*", "")
            
            // 10. 移除无序列表符号（- * + 列表项）
            .replaceAll("(?m)^[\\s]*[-*+]\\s+", "")
            
            // 11. 移除有序列表符号（1. 2. 3. 列表项）
            .replaceAll("(?m)^[\\s]*\\d+\\.\\s+", "")
            
            // 12. 移除水平分割线（--- 或 ***）
            .replaceAll("(?m)^\\s*[-*_]{3,}\\s*$", "")
            
            // 13. 移除表格分隔符（| 符号）
            .replaceAll("\\|", " ")
            
            // 14. 移除HTML标签
            .replaceAll("<[^>]+>", "")
            
            // 15. 清理多余的空白字符
            .replaceAll("\\s+", " ")
            
            // 16. 清理首尾空白
            .trim();

        log.debug("Markdown文本清理完成，清理后长度：{}，原始：{}字符 -> 清理后：{}字符", 
            cleanedText.length(), markdownText.length(), cleanedText.length());

        // 如果清理后文本为空，返回提示信息
        if (cleanedText.isEmpty()) {
            cleanedText = "内容无法转换为语音";
            log.warn("Markdown文本清理后为空，返回默认提示");
        }

        return cleanedText;
    }

    /**
     * 检查文本是否包含Markdown格式
     * 
     * @param text 待检查的文本
     * @return true如果包含Markdown格式符号，false否则
     */
    public static boolean containsMarkdown(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 检查常见的Markdown格式符号
        return text.matches(".*[#*_`\\[\\]()~>|].*") ||
               text.contains("```") ||
               text.matches(".*^\\d+\\.\\s+.*") ||
               text.matches(".*^[-*+]\\s+.*");
    }

    /**
     * 获取清理统计信息
     * 
     * @param originalText 原始文本
     * @param cleanedText 清理后文本
     * @return 清理统计信息
     */
    public static String getCleaningStats(String originalText, String cleanedText) {
        if (originalText == null) originalText = "";
        if (cleanedText == null) cleanedText = "";
        
        int originalLength = originalText.length();
        int cleanedLength = cleanedText.length();
        double reductionPercent = originalLength > 0 ? 
            ((double)(originalLength - cleanedLength) / originalLength) * 100 : 0;
        
        return String.format("原始文本：%d字符，清理后：%d字符，减少：%.1f%%", 
            originalLength, cleanedLength, reductionPercent);
    }
}
