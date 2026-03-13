package com.nexusvoice.infrastructure.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG查询规划器。
 * 在不引入额外模型调用的前提下，对用户问题做轻量意图识别和查询扩写，
 * 让检索链路具备接近多跳/多查询召回的能力。
 */
@Component
public class RagQueryPlanner {

    private static final Pattern COMPARISON_PATTERN = Pattern.compile("(.+?)(?:和|与|跟|及|vs|VS)(.+?)(?:的)?(?:区别|差异|不同|对比|比较)");
    private static final Pattern QUOTED_PATTERN = Pattern.compile("[\"“](.+?)[\"”]");
    private static final List<String> LEADING_PHRASES = List.of(
            "请", "麻烦", "帮我", "帮忙", "给我", "替我", "可以", "能否", "请问", "在知识库里", "在知识库中", "知识库里", "知识库中"
    );
    private static final List<String> QUERY_FILLERS = List.of(
            "为什么", "为何", "原因", "怎么", "如何", "哪些", "哪个", "什么", "一下", "一下子", "介绍", "总结", "概括", "对比", "比较",
            "区别", "差异", "相关", "内容", "信息", "资料", "情况", "问题", "请问", "帮我搜索", "帮我检索", "搜索一下", "检索一下",
            "查一下", "查找一下", "搜一下", "搜一搜"
    );
    private static final List<String> SPLIT_MARKERS = List.of(
            "转向", "形成", "体现", "原因", "区别", "对比", "比较", "启发", "总结", "核心", "重点", "主旨", "结论",
            "经历", "变化", "影响", "因为", "所以", "以及", "并且", "同时", "从", "到"
    );

    public RagQueryPlan plan(String originalQuery) {
        String normalizedQuery = normalize(originalQuery);
        QueryIntent intent = detectIntent(originalQuery, normalizedQuery);

        LinkedHashSet<String> retrievalQueries = new LinkedHashSet<>();
        if (!normalizedQuery.isBlank()) {
            retrievalQueries.add(normalizedQuery);
        }

        List<String> keywords = extractKeywords(normalizedQuery);
        retrievalQueries.addAll(keywords);
        retrievalQueries.addAll(buildIntentSpecificQueries(intent, normalizedQuery, keywords));

        retrievalQueries.removeIf(query -> query == null || query.isBlank());

        if (retrievalQueries.isEmpty() && originalQuery != null && !originalQuery.isBlank()) {
            retrievalQueries.add(originalQuery.trim());
        }

        return new RagQueryPlan(
                originalQuery == null ? "" : originalQuery.trim(),
                normalizedQuery,
                intent,
                new ArrayList<>(retrievalQueries),
                keywords
        );
    }

    public String normalize(String originalQuery) {
        if (originalQuery == null) {
            return "";
        }

        String normalized = originalQuery.trim();
        for (String phrase : LEADING_PHRASES) {
            if (normalized.startsWith(phrase)) {
                normalized = normalized.substring(phrase.length()).trim();
            }
        }

        normalized = normalized
                .replaceAll("^(搜索一下|搜索|检索一下|检索|查一下|查找一下|查找|找一下|找找|搜一下|搜一搜)", "")
                .replaceAll("^(关于|有关)", "")
                .replaceAll("(的信息|的内容|的资料|的介绍|相关内容|相关资料|相关信息)$", "")
                .replaceAll("[？?。！!]+$", "")
                .trim();

        return normalized.isEmpty() ? originalQuery.trim() : normalized;
    }

    private QueryIntent detectIntent(String originalQuery, String normalizedQuery) {
        String query = (originalQuery == null ? "" : originalQuery) + " " + normalizedQuery;
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        if (lowerQuery.contains("区别") || lowerQuery.contains("差异") || lowerQuery.contains("不同")
                || lowerQuery.contains("对比") || lowerQuery.contains("比较")) {
            return QueryIntent.COMPARISON;
        }
        if (lowerQuery.contains("为什么") || lowerQuery.contains("为何") || lowerQuery.contains("原因")
                || lowerQuery.contains("怎么形成") || lowerQuery.contains("如何形成") || lowerQuery.contains("怎么变")) {
            return QueryIntent.CAUSAL;
        }
        if (lowerQuery.contains("总结") || lowerQuery.contains("概括") || lowerQuery.contains("启发")
                || lowerQuery.contains("收获") || lowerQuery.contains("核心") || lowerQuery.contains("重点")
                || lowerQuery.contains("主旨") || lowerQuery.contains("结论")) {
            return QueryIntent.SUMMARIZATION;
        }
        if (lowerQuery.contains("哪些") || lowerQuery.contains("几个") || lowerQuery.contains("分别")
                || lowerQuery.contains("列出") || lowerQuery.contains("清单")) {
            return QueryIntent.LISTING;
        }
        return QueryIntent.FACT;
    }

    private List<String> buildIntentSpecificQueries(QueryIntent intent, String normalizedQuery, List<String> keywords) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        switch (intent) {
            case COMPARISON -> {
                queries.addAll(extractComparedEntities(normalizedQuery));
                if (keywords.size() >= 2) {
                    queries.add(String.join(" ", keywords.subList(0, Math.min(3, keywords.size()))));
                }
            }
            case CAUSAL -> {
                String stripped = normalizedQuery
                        .replace("为什么", "")
                        .replace("为何", "")
                        .replace("原因", "")
                        .replace("怎么", "")
                        .replace("如何", "")
                        .trim();
                if (!stripped.isBlank()) {
                    queries.add(stripped);
                }
                queries.addAll(extractComparedEntities(stripped));
            }
            case SUMMARIZATION -> {
                for (String keyword : keywords) {
                    queries.add(keyword);
                }
            }
            case LISTING -> {
                if (!keywords.isEmpty()) {
                    queries.add(String.join(" ", keywords));
                }
            }
            default -> queries.addAll(extractComparedEntities(normalizedQuery));
        }

        return new ArrayList<>(queries);
    }

    private List<String> extractComparedEntities(String query) {
        LinkedHashSet<String> entities = new LinkedHashSet<>();

        Matcher quotedMatcher = QUOTED_PATTERN.matcher(query);
        while (quotedMatcher.find()) {
            entities.add(quotedMatcher.group(1).trim());
        }

        Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(query);
        if (comparisonMatcher.find()) {
            entities.add(cleanEntity(comparisonMatcher.group(1)));
            entities.add(cleanEntity(comparisonMatcher.group(2)));
        }

        return entities.stream()
                .filter(entity -> entity != null && entity.length() >= 2)
                .limit(4)
                .toList();
    }

    private String cleanEntity(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("(之间|相比|比较|对比|区别|差异|是什么|有哪些)$", "").trim();
    }

    private List<String> extractKeywords(String normalizedQuery) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        String candidate = normalizedQuery;

        for (String filler : QUERY_FILLERS) {
            candidate = candidate.replace(filler, " ");
        }
        for (String marker : SPLIT_MARKERS) {
            candidate = candidate.replace(marker, " ");
        }

        candidate = candidate
                .replaceAll("[，,。；;：:！？?!（）()【】\\[\\]、/\\\\]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!candidate.isBlank()) {
            for (String part : candidate.split(" ")) {
                String keyword = part.trim();
                if (keyword.length() >= 2 && keyword.length() <= 20) {
                    keywords.add(keyword);
                }
            }
        }

        if (keywords.isEmpty() && normalizedQuery.length() >= 2) {
            keywords.add(normalizedQuery);
        }

        return new ArrayList<>(keywords).subList(0, Math.min(4, keywords.size()));
    }

    public enum QueryIntent {
        FACT,
        COMPARISON,
        CAUSAL,
        SUMMARIZATION,
        LISTING
    }

    public record RagQueryPlan(
            String originalQuery,
            String normalizedQuery,
            QueryIntent intent,
            List<String> retrievalQueries,
            List<String> keywords
    ) {
    }
}
