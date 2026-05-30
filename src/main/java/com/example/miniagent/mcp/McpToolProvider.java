package com.example.miniagent.mcp;

import com.example.miniagent.tool.Tool;

import java.util.ArrayList;
import java.util.List;

public class McpToolProvider {
    private final McpClientFactory clientFactory;

    public McpToolProvider(McpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<Tool> loadTools(List<McpServerConfig> configs) {
        List<Tool> tools = new ArrayList<>();
        for (McpServerConfig config : configs) {
            McpClient client = clientFactory.create(config);
            client.initialize();
            for (McpToolMetadata metadata : client.listTools()) {
                tools.add(new McpToolAdapter(client, withRegisteredName(config.name(), metadata)));
            }
        }
        return tools;
    }

    private McpToolMetadata withRegisteredName(String serverName, McpToolMetadata metadata) {
        return new McpToolMetadata(
                serverName,
                metadata.originalName(),
                "mcp_" + sanitize(serverName) + "_" + sanitize(metadata.originalName()),
                metadata.description(),
                metadata.inputSchema()
        );
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }
}
