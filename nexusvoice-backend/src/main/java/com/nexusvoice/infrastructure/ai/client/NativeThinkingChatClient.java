package com.nexusvoice.infrastructure.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.domain.conversation.constant.MessageRole;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 原生思考流客户端。
 * 用于对接返回 reasoning_content / thinking_blocks 的 OpenAI 兼容厂商扩展协议。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NativeThinkingChatClient {

    private final ObjectMapper objectMapper;

    public ChatResult chat(AiProvider provider, AiModel model, AiApiKey apiKey, ChatRequest request) throws Exception {
        String endpoint = resolveEndpoint(provider, model, apiKey);
        String payload = objectMapper.writeValueAsString(buildPayload(model, request, false));

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(resolveTimeout(model))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), response.body());

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        return new ChatResult(
                textValue(message.get("content")),
                extractReasoning(message),
                textValue(choice.get("finish_reason"))
        );
    }

    public void streamChat(AiProvider provider,
                           AiModel model,
                           AiApiKey apiKey,
                           ChatRequest request,
                           StreamListener listener) throws Exception {
        String endpoint = resolveEndpoint(provider, model, apiKey);
        String payload = objectMapper.writeValueAsString(buildPayload(model, request, true));

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(resolveTimeout(model))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient().send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            ensureSuccess(response.statusCode(), body);
        }

        StringBuilder fullContent = new StringBuilder();
        StringBuilder fullReasoning = new StringBuilder();
        String finishReason = "stop";
        boolean thinkingStarted = false;
        boolean thinkingEnded = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }

                JsonNode root = objectMapper.readTree(data);
                JsonNode choice = root.path("choices").path(0);
                JsonNode delta = choice.path("delta");

                String reasoningDelta = extractReasoning(delta);
                if (!reasoningDelta.isBlank()) {
                    if (!thinkingStarted) {
                        listener.onThinkingStart();
                        thinkingStarted = true;
                    }
                    fullReasoning.append(reasoningDelta);
                    listener.onThinkingDelta(reasoningDelta);
                }

                String contentDelta = textValue(delta.get("content"));
                if (!contentDelta.isBlank()) {
                    if (thinkingStarted && !thinkingEnded) {
                        listener.onThinkingEnd();
                        thinkingEnded = true;
                    }
                    fullContent.append(contentDelta);
                    listener.onContentDelta(contentDelta);
                }

                String currentFinishReason = textValue(choice.get("finish_reason"));
                if (!currentFinishReason.isBlank()) {
                    finishReason = currentFinishReason;
                }
            }
        }

        if (thinkingStarted && !thinkingEnded) {
            listener.onThinkingEnd();
        }
        listener.onComplete(new ChatResult(fullContent.toString(), fullReasoning.toString(), finishReason));
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private Duration resolveTimeout(AiModel model) {
        int seconds = model.getDefaultTimeoutSeconds() != null ? model.getDefaultTimeoutSeconds() : 120;
        return Duration.ofSeconds(Math.max(seconds, 30));
    }

    private String resolveEndpoint(AiProvider provider, AiModel model, AiApiKey apiKey) {
        String baseUrl = null;
        if (apiKey != null && apiKey.getBaseUrl() != null && !apiKey.getBaseUrl().isBlank()) {
            baseUrl = apiKey.getBaseUrl().trim();
        } else if (model.getDefaultBaseUrl() != null && !model.getDefaultBaseUrl().isBlank()) {
            baseUrl = model.getDefaultBaseUrl().trim();
        } else if (provider != null && provider.getDefaultBaseUrl() != null && !provider.getDefaultBaseUrl().isBlank()) {
            baseUrl = provider.getDefaultBaseUrl().trim();
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("未配置思考模型接口地址");
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        return baseUrl + "/chat/completions";
    }

    private Map<String, Object> buildPayload(AiModel model, ChatRequest request, boolean stream) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model.getModelCode());
        payload.put("messages", buildMessages(request.getMessages()));
        payload.put("stream", stream);

        if (request.getTemperature() != null) {
            payload.put("temperature", request.getTemperature());
        }
        if (request.getReasoningEffort() != null && !request.getReasoningEffort().isBlank()) {
            payload.put("reasoning_effort", request.getReasoningEffort());
        }

        String thinkingMode = normalizeThinkingMode(request.getThinkingMode());
        Integer maxTokens = request.getMaxTokens() != null
                ? request.getMaxTokens()
                : (model.getDefaultMaxTokens() != null ? model.getDefaultMaxTokens() : 2000);
        Map<String, Object> thinking = new LinkedHashMap<>();
        thinking.put("type", thinkingMode);
        if ("enabled".equals(thinkingMode) && request.getThinkingBudgetTokens() != null) {
            maxTokens = Math.max(maxTokens, request.getThinkingBudgetTokens() + 256);
            thinking.put("budget_tokens", request.getThinkingBudgetTokens());
        }
        payload.put("max_tokens", maxTokens);
        payload.put("thinking", thinking);
        return payload;
    }

    private List<Map<String, Object>> buildMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (messages == null) {
            return result;
        }

        for (ChatMessage message : messages) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", normalizeRole(message.getRole()));
            item.put("content", message.getContent());
            result.add(item);
        }
        return result;
    }

    private String normalizeRole(MessageRole role) {
        if (role == null) {
            return "user";
        }
        return role.name().toLowerCase();
    }

    private String normalizeThinkingMode(String value) {
        if (value == null || value.isBlank()) {
            return "disabled";
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "enabled", "auto", "disabled" -> normalized;
            default -> "disabled";
        };
    }

    private void ensureSuccess(int statusCode, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        String errorMessage = body;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.get("error");
            if (error != null && error.has("message")) {
                errorMessage = error.get("message").asText();
            } else if (root.has("message")) {
                errorMessage = root.get("message").asText();
            }
        } catch (Exception ignore) {
            // ignore
        }

        throw new IllegalStateException("思考模型请求失败（HTTP " + statusCode + "）: " + errorMessage);
    }

    private String extractReasoning(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "";
        }

        String reasoningContent = textValue(node.get("reasoning_content"));
        if (!reasoningContent.isBlank()) {
            return reasoningContent;
        }

        JsonNode thinkingBlocks = node.get("thinking_blocks");
        if (thinkingBlocks != null && thinkingBlocks.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode block : thinkingBlocks) {
                String thinking = textValue(block.get("thinking"));
                if (!thinking.isBlank()) {
                    builder.append(thinking);
                }
            }
            return builder.toString();
        }

        return "";
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        return node.asText("");
    }

    public record ChatResult(String content, String reasoningContent, String finishReason) {
    }

    public interface StreamListener {
        void onThinkingStart();

        void onThinkingDelta(String delta);

        void onThinkingEnd();

        void onContentDelta(String delta);

        void onComplete(ChatResult result);
    }
}
