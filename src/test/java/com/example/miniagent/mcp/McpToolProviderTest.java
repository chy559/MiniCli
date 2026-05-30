package com.example.miniagent.mcp;

import com.example.miniagent.tool.Tool;
import com.example.miniagent.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolProviderTest {

    @Test
    void shouldWrapDiscoveredMcpToolsWithServerPrefix() {
        FakeMcpClient client = new FakeMcpClient();
        McpToolProvider provider = new McpToolProvider(config -> client);

        List<Tool> tools = provider.loadTools(List.of(McpServerConfig.stdio("github", "fake", List.of(), Map.of())));
        Tool tool = tools.get(0);
        ToolResult result = tool.execute(Map.of("query", "bugs"));

        assertEquals("mcp_github_search_issues", tool.name());
        assertTrue(tool.description().contains("MCP server: github"));
        assertEquals(Map.of("type", "object"), tool.inputSchema());
        assertEquals("called search_issues with bugs", result.content());
        assertTrue(client.initialized);
    }

    private static class FakeMcpClient implements McpClient {
        private boolean initialized;

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public List<McpToolMetadata> listTools() {
            return List.of(new McpToolMetadata(
                    "github",
                    "search_issues",
                    "search_issues",
                    "Search issues",
                    Map.of("type", "object")
            ));
        }

        @Override
        public String callTool(String toolName, Map<String, Object> arguments) {
            return "called " + toolName + " with " + arguments.get("query");
        }

        @Override
        public void close() {
        }
    }
}
