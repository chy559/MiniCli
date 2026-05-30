package com.example.miniagent.mcp;

public interface McpClientFactory {
    McpClient create(McpServerConfig config);
}
