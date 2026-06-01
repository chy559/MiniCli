package com.example.miniagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;

public class StdioMcpClient extends JsonRpcMcpClient {

    public StdioMcpClient(McpServerConfig config) {
        super(config, new StdioMcpTransport(config));
    }

    public StdioMcpClient(McpServerConfig config, ObjectMapper objectMapper) {
        super(config, new StdioMcpTransport(config, objectMapper), objectMapper);
    }
}
