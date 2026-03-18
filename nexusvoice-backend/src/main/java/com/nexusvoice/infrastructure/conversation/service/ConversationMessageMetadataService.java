package com.nexusvoice.infrastructure.conversation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.rag.model.vo.RagCitation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话消息metadata读写服务。
 * 统一维护引用、思考过程等可扩展字段，避免后续字段互相覆盖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMessageMetadataService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final TypeReference<List<RagCitation>> CITATION_LIST_TYPE = new TypeReference<>() { };
    private static final String METADATA_TYPE = "conversation_message_metadata_v1";
    private static final String CITATIONS_KEY = "citations";
    private static final String REASONING_KEY = "reasoningContent";

    private final ObjectMapper objectMapper;

    public String writeMetadata(List<RagCitation> citations, String reasoningContent) {
        boolean hasCitations = citations != null && !citations.isEmpty();
        boolean hasReasoning = reasoningContent != null && !reasoningContent.isBlank();
        if (!hasCitations && !hasReasoning) {
            return null;
        }

        try {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("type", METADATA_TYPE);
            if (hasCitations) {
                metadata.put(CITATIONS_KEY, citations);
            }
            if (hasReasoning) {
                metadata.put(REASONING_KEY, reasoningContent);
            }
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            log.warn("写入消息metadata失败: {}", e.getMessage());
            return null;
        }
    }

    public List<RagCitation> readCitations(String metadata) {
        Map<String, Object> parsed = parseMetadata(metadata);
        if (parsed.isEmpty()) {
            return List.of();
        }

        Object rawCitations = parsed.get(CITATIONS_KEY);
        if (rawCitations == null) {
            return List.of();
        }

        try {
            return objectMapper.convertValue(rawCitations, CITATION_LIST_TYPE);
        } catch (Exception e) {
            log.debug("解析消息metadata中的citations失败: {}", e.getMessage());
            return List.of();
        }
    }

    public String readReasoningContent(String metadata) {
        Map<String, Object> parsed = parseMetadata(metadata);
        if (parsed.isEmpty()) {
            return null;
        }

        Object reasoning = parsed.get(REASONING_KEY);
        if (reasoning == null) {
            return null;
        }
        String value = String.valueOf(reasoning).trim();
        return value.isEmpty() ? null : value;
    }

    private Map<String, Object> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, MAP_TYPE);
        } catch (Exception e) {
            log.debug("解析消息metadata失败，忽略该字段: {}", e.getMessage());
            return Map.of();
        }
    }
}
