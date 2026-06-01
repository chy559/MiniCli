package com.example.miniagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpConfigLoader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<McpServerConfig> load(Path configPath) {
        if (Files.notExists(configPath)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(Files.readString(configPath));
            List<McpServerConfig> configs = new ArrayList<>();
            for (JsonNode server : root.path("servers")) {
                String name = server.path("name").asText();
                String transport = server.path("transport").asText("stdio");
                if (isStreamableHttp(transport)) {
                    Map<String, String> headers = new HashMap<>();
                    server.path("headers").fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
                    configs.add(McpServerConfig.streamableHttp(name, server.path("url").asText(), headers));
                    continue;
                }
                if (!"stdio".equalsIgnoreCase(transport)) {
                    continue;
                }
                List<String> args = new ArrayList<>();
                for (JsonNode arg : server.path("args")) {
                    args.add(arg.asText());
                }
                Map<String, String> env = new HashMap<>();
                server.path("env").fields().forEachRemaining(entry -> env.put(entry.getKey(), entry.getValue().asText()));
                configs.add(McpServerConfig.stdio(name, server.path("command").asText(), args, env));
            }
            return configs;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load MCP config: " + configPath, e);
        }
    }

    private boolean isStreamableHttp(String transport) {
        return "streamable_http".equalsIgnoreCase(transport)
                || "streamable-http".equalsIgnoreCase(transport)
                || "http".equalsIgnoreCase(transport);
    }
}
