package com.example.miniagent.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleClient implements LlmClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleClient(HttpClient httpClient, ObjectMapper objectMapper, String baseUrl, String apiKey, String model) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public static OpenAiCompatibleClient fromEnvironment() {
        String apiKey = System.getenv("MINI_AGENT_API_KEY");
        String baseUrl = System.getenv().getOrDefault("MINI_AGENT_BASE_URL", "https://api.openai.com/v1");
        String model = System.getenv().getOrDefault("MINI_AGENT_MODEL", "gpt-4o-mini");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing MINI_AGENT_API_KEY environment variable.");
        }
        return new OpenAiCompatibleClient(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build(),
                new ObjectMapper(), baseUrl, apiKey, model);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> conversationHistory, List<ToolDefinition> toolDefinitions) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", toMessages(conversationHistory));
            if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
                payload.put("tools", toTools(toolDefinitions));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("LLM request failed: " + response.statusCode() + " " + response.body());
            }
            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request failed", e);
        }
    }

    private List<Map<String, Object>> toMessages(List<ChatMessage> conversationHistory) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage message : conversationHistory) {
            Map<String, Object> item = new HashMap<>();
            item.put("role", message.role().name().toLowerCase());
            item.put("content", message.content());
            if (message.name() != null) {
                item.put("name", message.name());
            }
            if (message.toolCallId() != null) {
                item.put("tool_call_id", message.toolCallId());
            }
            messages.add(item);
        }
        return messages;
    }

    private List<Map<String, Object>> toTools(List<ToolDefinition> toolDefinitions) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (ToolDefinition definition : toolDefinitions) {
            Map<String, Object> function = new HashMap<>();
            function.put("name", definition.name());
            function.put("description", definition.description());
            function.put("parameters", definition.inputSchema());

            Map<String, Object> tool = new HashMap<>();
            tool.put("type", "function");
            tool.put("function", function);
            tools.add(tool);
        }
        return tools;
    }

    private ChatResponse parseResponse(String body) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode message = root.path("choices").path(0).path("message");
        String content = message.path("content").isNull() ? "" : message.path("content").asText("");

        List<ToolCall> toolCalls = new ArrayList<>();
        for (JsonNode toolCallNode : message.path("tool_calls")) {
            String id = toolCallNode.path("id").asText();
            JsonNode function = toolCallNode.path("function");
            String name = function.path("name").asText();
            Map<String, Object> args = objectMapper.readValue(function.path("arguments").asText("{}"), Map.class);
            toolCalls.add(new ToolCall(id, name, args));
        }
        return new ChatResponse(content, toolCalls);
    }
}
