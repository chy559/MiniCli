package com.example.miniagent.mcp;

import java.util.List;
import java.util.Map;

public interface McpClient extends AutoCloseable {
    void initialize();

    List<McpToolMetadata> listTools();

    String callTool(String toolName, Map<String, Object> arguments);

    @Override
    void close();
}
