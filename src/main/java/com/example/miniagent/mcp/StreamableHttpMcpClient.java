package com.example.miniagent.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

public class StreamableHttpMcpClient extends JsonRpcMcpClient {

    public StreamableHttpMcpClient(McpServerConfig config) {
        this(config, new OkHttpClient(), new ObjectMapper());
    }

    public StreamableHttpMcpClient(McpServerConfig config, OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        super(config, new StreamableHttpMcpTransport(config, okHttpClient, objectMapper), objectMapper);
    }
}
