package com.example.miniagent.mcp;

public class DefaultMcpClientFactory implements McpClientFactory {

    @Override
    public McpClient create(McpServerConfig config) {
        if (isStreamableHttp(config.transport())) {
            return new StreamableHttpMcpClient(config);
        }
        return new StdioMcpClient(config);
    }

    private boolean isStreamableHttp(String transport) {
        return "streamable_http".equalsIgnoreCase(transport)
                || "streamable-http".equalsIgnoreCase(transport)
                || "http".equalsIgnoreCase(transport);
    }
}
