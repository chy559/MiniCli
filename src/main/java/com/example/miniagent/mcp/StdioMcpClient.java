package com.example.miniagent.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class StdioMcpClient implements McpClient {
    private final McpServerConfig config;
    private final ObjectMapper objectMapper;
    private final AtomicLong nextId = new AtomicLong(1);
    private Process process;
    private BufferedInputStream input;
    private BufferedOutputStream output;

    public StdioMcpClient(McpServerConfig config) {
        this(config, new ObjectMapper());
    }

    public StdioMcpClient(McpServerConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public synchronized void initialize() {
        ensureStarted();
        Map<String, Object> result = request("initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "mini-agent-cli", "version", "0.1.0")
        ));
        if (result == null) {
            throw new IllegalStateException("MCP initialize failed for server: " + config.name());
        }
        notify("notifications/initialized", Map.of());
    }

    @Override
    public synchronized List<McpToolMetadata> listTools() {
        ensureStarted();
        Map<String, Object> result = request("tools/list", Map.of());
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
        ensureStarted();
        Map<String, Object> result = request("tools/call", Map.of(
                "name", toolName,
                "arguments", arguments == null ? Map.of() : arguments
        ));
        return formatToolResult(objectMapper.valueToTree(result));
    }

    @Override
    public synchronized void close() {
        if (process != null) {
            process.destroy();
        }
    }

    private void ensureStarted() {
        if (process != null && process.isAlive()) {
            return;
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(config.commandLine());
            builder.environment().putAll(config.expandedEnv());
            process = builder.start();
            input = new BufferedInputStream(process.getInputStream());
            output = new BufferedOutputStream(process.getOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start MCP server: " + config.name(), e);
        }
    }

    private Map<String, Object> request(String method, Map<String, Object> params) {
        long id = nextId.getAndIncrement();
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.put("params", params);
        writeMessage(request);

        while (true) {
            JsonNode response = readMessage();
            if (response == null || !response.has("id") || response.path("id").asLong() != id) {
                continue;
            }
            if (response.has("error")) {
                throw new IllegalStateException("MCP request failed: " + response.path("error"));
            }
            return objectMapper.convertValue(response.path("result"), Map.class);
        }
    }

    private void notify(String method, Map<String, Object> params) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
        notification.put("params", params);
        writeMessage(notification);
    }

    private void writeMessage(Map<String, Object> message) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(message);
            byte[] header = ("Content-Length: " + payload.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            output.write(header);
            output.write(payload);
            output.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write MCP message", e);
        }
    }

    private JsonNode readMessage() {
        try {
            int contentLength = readContentLength();
            byte[] payload = input.readNBytes(contentLength);
            if (payload.length != contentLength) {
                throw new IllegalStateException("Unexpected EOF while reading MCP message");
            }
            return objectMapper.readTree(payload);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read MCP message", e);
        }
    }

    private int readContentLength() throws IOException {
        String line;
        int contentLength = -1;
        while ((line = readHeaderLine()) != null) {
            if (line.isBlank()) {
                break;
            }
            String lower = line.toLowerCase();
            if (lower.startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
            }
        }
        if (contentLength < 0) {
            throw new IllegalStateException("MCP message missing Content-Length header");
        }
        return contentLength;
    }

    private String readHeaderLine() throws IOException {
        StringBuilder builder = new StringBuilder();
        int current;
        while ((current = input.read()) != -1) {
            if (current == '\r') {
                int next = input.read();
                if (next == '\n') {
                    break;
                }
                builder.append((char) current);
                if (next != -1) {
                    builder.append((char) next);
                }
                continue;
            }
            if (current == '\n') {
                break;
            }
            builder.append((char) current);
        }
        if (current == -1 && builder.isEmpty()) {
            return null;
        }
        return builder.toString();
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
