package com.example.miniagent.mcp;

import com.example.miniagent.tool.Tool;
import com.example.miniagent.tool.ToolPermission;
import com.example.miniagent.tool.ToolResult;

import java.util.Map;

public class McpToolAdapter implements Tool {
    private final McpClient client;
    private final McpToolMetadata metadata;

    public McpToolAdapter(McpClient client, McpToolMetadata metadata) {
        this.client = client;
        this.metadata = metadata;
    }

    @Override
    public String name() {
        return metadata.registeredName();
    }

    @Override
    public String description() {
        return "[MCP server: %s] %s".formatted(metadata.serverName(), metadata.description());
    }

    @Override
    public Map<String, Object> inputSchema() {
        return metadata.inputSchema();
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String result = client.callTool(metadata.originalName(), args);
        return new ToolResult(name(), result, true);
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.EXTERNAL;
    }
}
