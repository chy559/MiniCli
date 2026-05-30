package com.example.miniagent.mcp;

import java.util.Map;

public record McpToolMetadata(
        String serverName,
        String originalName,
        String registeredName,
        String description,
        Map<String, Object> inputSchema
) {
}
