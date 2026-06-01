package com.example.miniagent.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonRpcMcpClient implements McpClient {
    private final McpServerConfig config;
    private final McpTransport transport;
    private final ObjectMapper objectMapper;

    public JsonRpcMcpClient(McpServerConfig config, McpTransport transport) {
        this(config, transport, new ObjectMapper());
    }

    public JsonRpcMcpClient(McpServerConfig config, McpTransport transport, ObjectMapper objectMapper) {
        this.config = config;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void initialize() {
        transport.start();
        Map<String, Object> result = transport.request("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "mini-agent-cli", "version", "0.1.0")
        ));
        if (result == null) {
            throw new IllegalStateException("MCP initialize failed for server: " + config.name());
        }
        transport.notify("notifications/initialized", Map.of());
    }

    @Override
    public synchronized List<McpToolMetadata> listTools() {
        Map<String, Object> result = transport.request("tools/list", Map.of());
        JsonNode root = objectMapper.valueToTree(result);
        List<McpToolMetadata> tools = new ArrayList<>();
        for (JsonNode tool : root.path("tools")) {
            Map<String, Object> inputSchema = objectMapper.convertValue(tool.path("inputSchema"), Map.class);
            tools.add(new McpToolMetadata(
                    config.name(),
                    tool.path("name").asText(),
                    tool.path("name").asText(),
                    tool.path("description").asText(""),
                    inputSchema.isEmpty() ? Map.of("type", "object", "properties", Map.of()) : inputSchema
            ));
        }
        return tools;
    }

    @Override
    public synchronized String callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> result = transport.request("tools/call", Map.of(
                "name", toolName,
                "arguments", arguments == null ? Map.of() : arguments
        ));
        return formatToolResult(objectMapper.valueToTree(result));
    }

    @Override
    public synchronized void close() {
        transport.close();
    }

    private String formatToolResult(JsonNode result) {
        StringBuilder builder = new StringBuilder();
        for (JsonNode item : result.path("content")) {
            if ("text".equals(item.path("type").asText())) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator());
                }
                builder.append(item.path("text").asText());
            }
        }
        if (!builder.isEmpty()) {
            return builder.toString();
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return String.valueOf(result);
        }
    }
}
