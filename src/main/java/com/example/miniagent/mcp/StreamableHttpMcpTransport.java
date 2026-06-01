package com.example.miniagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StreamableHttpMcpTransport implements McpTransport {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final McpServerConfig config;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final AtomicLong nextId = new AtomicLong(1);

    public StreamableHttpMcpTransport(McpServerConfig config, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        if (config.url() == null || config.url().isBlank()) {
            throw new IllegalStateException("Streamable HTTP MCP server requires url: " + config.name());
        }
    }

    @Override
    public synchronized Map<String, Object> request(String method, Map<String, Object> params) {
        start();
        long id = nextId.getAndIncrement();
        Map<String, Object> message = new HashMap<>();
        message.put("jsonrpc", "2.0");
        message.put("id", id);
        message.put("method", method);
        message.put("params", params);

        JsonNode response = send(message);
        if (response.has("error")) {
            throw new IllegalStateException("MCP request failed: " + response.path("error"));
        }
        return objectMapper.convertValue(response.path("result"), Map.class);
    }

    @Override
    public synchronized void notify(String method, Map<String, Object> params) {
        start();
        Map<String, Object> message = new HashMap<>();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        message.put("params", params);
        send(message);
    }

    @Override
    public void close() {
    }

    private JsonNode send(Map<String, Object> message) {
        try {
            RequestBody body = RequestBody.create(objectMapper.writeValueAsBytes(message), JSON);
            Request.Builder builder = new Request.Builder()
                    .url(config.url())
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .header("MCP-Protocol-Version", "2024-11-05");
            config.expandedHeaders().forEach(builder::header);

            try (Response response = okHttpClient.newCall(builder.build()).execute()) {
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("MCP HTTP request failed: " + response.code() + " " + response.message());
                }
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return objectMapper.createObjectNode();
                }
                String payload = responseBody.string();
                String contentType = response.header("Content-Type", "");
                if (contentType.toLowerCase().contains("text/event-stream")) {
                    payload = extractFirstSseData(payload);
                }
                if (payload == null || payload.isBlank()) {
                    return objectMapper.createObjectNode();
                }
                return objectMapper.readTree(payload);
            }
        } catch (IOException e) {
            throw new IllegalStateException("MCP HTTP request failed", e);
        }
    }

    private String extractFirstSseData(String payload) {
        StringBuilder builder = new StringBuilder();
        for (String line : payload.split("\\R")) {
            if (line.startsWith("data:")) {
                builder.append(line.substring("data:".length()).trim());
            }
            if (line.isBlank() && !builder.isEmpty()) {
                break;
            }
        }
        return builder.toString();
    }
}
