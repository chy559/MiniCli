package com.example.miniagent.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DefaultMcpClientFactoryTest {

    @Test
    void shouldCreateStreamableHttpClientForHttpTransport() {
        McpClient client = new DefaultMcpClientFactory().create(
                McpServerConfig.streamableHttp("remote", "https://example.com/mcp", Map.of())
        );

        assertInstanceOf(StreamableHttpMcpClient.class, client);
    }

    @Test
    void shouldCreateStdioClientForStdioTransport() {
        McpClient client = new DefaultMcpClientFactory().create(
                McpServerConfig.stdio("local", "fake", java.util.List.of(), Map.of())
        );

        assertInstanceOf(StdioMcpClient.class, client);
    }
}
