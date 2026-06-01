package com.example.miniagent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamableHttpMcpTransportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCallStreamableHttpServerWithJsonRpc() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(jsonResponse("""
                    {"jsonrpc":"2.0","id":1,"result":{"capabilities":{}}}
                    """));
            server.enqueue(jsonResponse("""
                    {"jsonrpc":"2.0","result":{}}
                    """));
            server.enqueue(jsonResponse("""
                    {"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"echo","description":"Echo","inputSchema":{"type":"object"}}]}}
                    """));
            server.enqueue(jsonResponse("""
                    {"jsonrpc":"2.0","id":3,"result":{"content":[{"type":"text","text":"hello"}]}}
                    """));

            McpServerConfig config = McpServerConfig.streamableHttp(
                    "remote",
                    server.url("/mcp").toString(),
                    Map.of("Authorization", "Bearer test")
            );
            McpClient client = new StreamableHttpMcpClient(config, new OkHttpClient(), objectMapper);

            client.initialize();
            List<McpToolMetadata> tools = client.listTools();
            String result = client.callTool("echo", Map.of("message", "hello"));

            assertEquals("echo", tools.get(0).originalName());
            assertEquals("hello", result);

            RecordedRequest initialize = server.takeRequest();
            assertEquals("POST", initialize.getMethod());
            assertTrue(initialize.getHeader("Content-Type").startsWith("application/json"));
            assertTrue(initialize.getHeader("Accept").contains("text/event-stream"));
            assertEquals("2024-11-05", initialize.getHeader("MCP-Protocol-Version"));
            assertEquals("Bearer test", initialize.getHeader("Authorization"));

            JsonNode body = objectMapper.readTree(initialize.getBody().readUtf8());
            assertEquals("initialize", body.path("method").asText());
            assertEquals(1, body.path("id").asInt());
        }
    }

    @Test
    void shouldParseSseDataResponse() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("""
                            event: message
                            data: {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"from sse"}]}}

                            """));

            StreamableHttpMcpTransport transport = new StreamableHttpMcpTransport(
                    McpServerConfig.streamableHttp("remote", server.url("/mcp").toString(), Map.of()),
                    new OkHttpClient(),
                    objectMapper
            );

            Map<String, Object> result = transport.request("tools/call", Map.of("name", "echo"));

            JsonNode root = objectMapper.valueToTree(result);
            assertEquals("from sse", root.path("content").get(0).path("text").asText());
        }
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
