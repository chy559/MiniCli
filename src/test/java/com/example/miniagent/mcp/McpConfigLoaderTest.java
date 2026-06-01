package com.example.miniagent.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadStdioServerConfig() throws IOException {
        Path config = tempDir.resolve("mcp.json");
        Files.writeString(config, """
                {
                  "servers": [
                    {
                      "name": "github",
                      "transport": "stdio",
                      "command": "npx",
                      "args": ["-y", "@modelcontextprotocol/server-github"],
                      "env": {
                        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
                      }
                    }
                  ]
                }
                """);

        List<McpServerConfig> configs = new McpConfigLoader().load(config);

        assertEquals(1, configs.size());
        assertEquals("github", configs.get(0).name());
        assertEquals("npx", configs.get(0).command());
        assertEquals(List.of("-y", "@modelcontextprotocol/server-github"), configs.get(0).args());
    }

    @Test
    void shouldLoadStreamableHttpServerConfig() throws IOException {
        Path config = tempDir.resolve("mcp.json");
        Files.writeString(config, """
                {
                  "servers": [
                    {
                      "name": "remote",
                      "transport": "streamable_http",
                      "url": "https://example.com/mcp",
                      "headers": {
                        "Authorization": "Bearer ${MCP_TOKEN}"
                      }
                    }
                  ]
                }
                """);

        List<McpServerConfig> configs = new McpConfigLoader().load(config);

        assertEquals(1, configs.size());
        assertEquals("remote", configs.get(0).name());
        assertEquals("streamable_http", configs.get(0).transport());
        assertEquals("https://example.com/mcp", configs.get(0).url());
        assertEquals("Bearer ${MCP_TOKEN}", configs.get(0).headers().get("Authorization"));
    }
}
