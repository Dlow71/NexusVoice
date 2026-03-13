package com.nexusvoice.infrastructure.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.domain.rag.model.vo.RagCitation;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG引用解析与标准化服务。
 * 负责把提示词里的资料块解析为结构化引用，并把回答中的[资料N]重写为[来源N]。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagCitationService {

    private static final Pattern SNIPPET_PATTERN = Pattern.compile(
            "(?ms)^\\d+\\. \\[资料(\\d+)] (.*?)\\n(.*?)(?=^\\d+\\. \\[资料|^【回答要求】)"
    );
    private static final Pattern LEGACY_REFERENCE_PATTERN = Pattern.compile("\\[资料(\\d+)]");
    private static final Pattern NORMALIZED_REFERENCE_PATTERN = Pattern.compile("\\[来源(\\d+)]");
    private static final int MAX_FALLBACK_CITATIONS = 3;
    private static final int MAX_SNIPPET_LENGTH = 260;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<RagCitation>> CITATION_LIST_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public RagAnswerPackage buildAnswerPackage(ChatRequest request, String content) {
        if (content == null) {
            return new RagAnswerPackage("", List.of());
        }

        List<RagCitation> parsedCitations = parseCitations(request);
        if (parsedCitations.isEmpty()) {
            return new RagAnswerPackage(content, List.of());
        }

        LinkedHashMap<Integer, Integer> sourceOrder = collectReferenceOrder(content);
        String normalizedContent = rewriteReferences(content, sourceOrder);
        List<RagCitation> selectedCitations = selectCitations(parsedCitations, sourceOrder);
        return new RagAnswerPackage(normalizedContent, selectedCitations);
    }

    public String writeMetadata(List<RagCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("type", "rag_citations_v1");
            metadata.put("citations", citations);
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("写入RAG引用metadata失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<RagCitation> readMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(metadata, MAP_TYPE);
            Object rawCitations = parsed.get("citations");
            if (rawCitations == null) {
                return List.of();
            }
            return objectMapper.convertValue(rawCitations, CITATION_LIST_TYPE);
        } catch (Exception e) {
            log.debug("解析RAG引用metadata失败，忽略该字段: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RagCitation> parseCitations(ChatRequest request) {
        if (request == null || request.getMessages() == null) {
            return List.of();
        }

        String ragPrompt = request.getMessages().stream()
                .filter(message -> message.getRole() == MessageRole.SYSTEM)
                .map(ChatMessage::getContent)
                .filter(content -> content != null && content.contains("【知识库检索结果】"))
                .findFirst()
                .orElse(null);
        if (ragPrompt == null || ragPrompt.isBlank()) {
            return List.of();
        }

        List<RagCitation> citations = new ArrayList<>();
        Matcher matcher = SNIPPET_PATTERN.matcher(ragPrompt);
        while (matcher.find()) {
            Integer sourceIndex = Integer.valueOf(matcher.group(1));
            String header = matcher.group(2);
            String body = matcher.group(3);
            citations.add(RagCitation.builder()
                    .originalSourceIndex(sourceIndex)
                    .knowledgeBaseName(extractHeaderValue(header, "知识库", List.of("知识库ID", "文件ID", "文件")))
                    .knowledgeBaseId(parseLong(extractHeaderValue(header, "知识库ID", List.of("文件ID", "文件", "片段位置", "片段范围", "分数"))))
                    .fileId(parseLong(extractHeaderValue(header, "文件ID", List.of("文件", "片段位置", "片段范围", "分数"))))
                    .fileName(extractHeaderValue(header, "文件", List.of("片段位置", "片段范围", "分数")))
                    .location(resolveLocation(header))
                    .score(parseDouble(extractHeaderValue(header, "分数", List.of("命中查询"))))
                    .matchedQueries(parseMatchedQueries(extractHeaderValue(header, "命中查询", List.of())))
                    .snippet(cleanSnippet(body))
                    .build());
        }
        return citations;
    }

    private LinkedHashMap<Integer, Integer> collectReferenceOrder(String content) {
        LinkedHashMap<Integer, Integer> sourceOrder = new LinkedHashMap<>();

        Matcher legacyMatcher = LEGACY_REFERENCE_PATTERN.matcher(content);
        while (legacyMatcher.find()) {
            Integer sourceIndex = Integer.valueOf(legacyMatcher.group(1));
            sourceOrder.computeIfAbsent(sourceIndex, ignored -> sourceOrder.size() + 1);
        }

        if (!sourceOrder.isEmpty()) {
            return sourceOrder;
        }

        Matcher normalizedMatcher = NORMALIZED_REFERENCE_PATTERN.matcher(content);
        while (normalizedMatcher.find()) {
            Integer sourceIndex = Integer.valueOf(normalizedMatcher.group(1));
            sourceOrder.computeIfAbsent(sourceIndex, ignored -> sourceOrder.size() + 1);
        }
        return sourceOrder;
    }

    private String rewriteReferences(String content, LinkedHashMap<Integer, Integer> sourceOrder) {
        if (content == null || content.isBlank()) {
            return content;
        }
        Matcher matcher = LEGACY_REFERENCE_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Integer sourceIndex = Integer.valueOf(matcher.group(1));
            Integer normalizedIndex = sourceOrder.computeIfAbsent(sourceIndex, ignored -> sourceOrder.size() + 1);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("[来源" + normalizedIndex + "]"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private List<RagCitation> selectCitations(List<RagCitation> parsedCitations,
                                              LinkedHashMap<Integer, Integer> sourceOrder) {
        Map<Integer, RagCitation> citationMap = new LinkedHashMap<>();
        for (RagCitation citation : parsedCitations) {
            if (citation.getOriginalSourceIndex() != null) {
                citationMap.put(citation.getOriginalSourceIndex(), citation);
            }
        }

        LinkedHashSet<Integer> selectedSources = new LinkedHashSet<>();
        if (!sourceOrder.isEmpty()) {
            selectedSources.addAll(sourceOrder.keySet());
        } else {
            parsedCitations.stream()
                    .map(RagCitation::getOriginalSourceIndex)
                    .limit(MAX_FALLBACK_CITATIONS)
                    .forEach(selectedSources::add);
        }

        List<RagCitation> result = new ArrayList<>();
        int displayIndex = 1;
        for (Integer sourceIndex : selectedSources) {
            RagCitation citation = citationMap.get(sourceIndex);
            if (citation == null) {
                continue;
            }
            result.add(citation.toBuilder()
                    .id("source-" + displayIndex)
                    .label("来源" + displayIndex)
                    .build());
            displayIndex++;
        }
        return result;
    }

    private String extractHeaderValue(String header, String key, List<String> nextKeys) {
        if (header == null || header.isBlank()) {
            return null;
        }
        int start = header.indexOf(key + "=");
        if (start < 0) {
            return null;
        }
        start += key.length() + 1;
        int end = header.length();
        for (String nextKey : nextKeys) {
            int candidate = header.indexOf("，" + nextKey + "=", start);
            if (candidate >= 0 && candidate < end) {
                end = candidate;
            }
        }
        return header.substring(start, end).trim();
    }

    private String resolveLocation(String header) {
        String exactLocation = extractHeaderValue(header, "片段位置", List.of("分数", "命中查询"));
        if (exactLocation != null && !exactLocation.isBlank()) {
            return exactLocation;
        }
        return extractHeaderValue(header, "片段范围", List.of("分数", "命中查询"));
    }

    private List<String> parseMatchedQueries(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> queries = new ArrayList<>();
        for (String item : value.split("\\s*/\\s*")) {
            String trimmed = item.trim();
            if (!trimmed.isBlank()) {
                queries.add(trimmed);
            }
        }
        return queries;
    }

    private String cleanSnippet(String body) {
        if (body == null) {
            return null;
        }
        String normalized = body
                .replace('\r', '\n')
                .replaceAll("(?m)^\\[片段\\d+(?:·命中)?]\\s*", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        return normalizeMarkdownArtifacts(truncateSnippet(normalized));
    }

    private String truncateSnippet(String text) {
        if (text == null || text.isBlank() || text.length() <= MAX_SNIPPET_LENGTH) {
            return text;
        }

        List<String> blocks = splitBlocks(text);
        StringBuilder builder = new StringBuilder();
        for (String block : blocks) {
            if (block == null || block.isBlank()) {
                continue;
            }
            String candidate = builder.isEmpty() ? block.trim() : builder + "\n\n" + block.trim();
            if (candidate.length() > MAX_SNIPPET_LENGTH) {
                if (builder.isEmpty()) {
                    return text.substring(0, MAX_SNIPPET_LENGTH).trim() + "...";
                }
                return builder.toString().trim() + "...";
            }
            builder.setLength(0);
            builder.append(candidate);
        }
        return builder.length() > 0 ? builder.toString().trim() : text.substring(0, MAX_SNIPPET_LENGTH).trim() + "...";
    }

    private List<String> splitBlocks(String text) {
        String[] parts = text.split("\\n\\s*\\n");
        List<String> blocks = new ArrayList<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private String normalizeMarkdownArtifacts(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text
                .replaceAll("^\"+|\"+$", "")
                .replaceAll("(?m)^(#+)([^\\s#])", "$1 $2")
                .replaceAll("(?m)^>\\s*\"?", "> ")
                .replaceAll("\\*\\*\\s+(?=\\S)", "")
                .replaceAll("(?<=\\S)\\s+\\*\\*", "")
                .replaceAll("(?<!\\*)\\*(?!\\*)", "")
                .replaceAll("(?<!`)`(?!`)", "")
                .replaceAll("\\n{3,}", "\n\n");

        normalized = trimUnbalancedDoubleMarker(normalized, "**");
        normalized = trimUnbalancedDoubleMarker(normalized, "__");
        return normalized.trim();
    }

    private String trimUnbalancedDoubleMarker(String text, String marker) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int occurrences = countOccurrences(text, marker);
        if (occurrences % 2 == 0) {
            return text;
        }
        int trailingIndex = text.lastIndexOf(marker);
        if (trailingIndex >= 0) {
            return (text.substring(0, trailingIndex) + text.substring(trailingIndex + marker.length()))
                    .trim();
        }
        return text;
    }

    private int countOccurrences(String text, String marker) {
        int count = 0;
        int index = 0;
        while (index >= 0) {
            index = text.indexOf(marker, index);
            if (index >= 0) {
                count++;
                index += marker.length();
            }
        }
        return count;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public record RagAnswerPackage(String content, List<RagCitation> citations) {
    }
}
