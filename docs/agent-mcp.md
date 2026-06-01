# MCP Integration

## Purpose

MCP support lets external MCP servers contribute tools to the existing agent tool layer without changing `Agent`.

The core rule:

```text
MCP tool -> McpToolAdapter -> ToolRegistry -> normal LLM tool calling
```

## Config

Default path:

```text
~/.mini-agent/mcp.json
```

Example:

```json
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
```

`stdio` and `streamable_http` transports are implemented. Environment values like `${GITHUB_TOKEN}` are expanded from the process environment.

Streamable HTTP example:

```json
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
```

## Startup Flow

`Main` loads MCP config after registering local tools:

```text
McpConfigLoader.load
  -> McpToolProvider.loadTools
  -> DefaultMcpClientFactory
  -> StdioMcpClient or StreamableHttpMcpClient
  -> JsonRpcMcpClient.initialize
  -> JsonRpcMcpClient.listTools
  -> McpToolAdapter
  -> ToolRegistry.register
```

MCP tools are named with a prefix:

```text
mcp_<server>_<tool>
```

For example:

```text
mcp_github_search_issues
```

This avoids collisions with local tools such as `read_file`, `search_code`, or another MCP server's `search` tool.

## Runtime Flow

At model call time, MCP tools are indistinguishable from local tools:

```text
Agent
  -> toolRegistry.getToolDefinitions()
  -> LLM chooses a tool
  -> ToolRegistry.executeTools
  -> McpToolAdapter.execute
  -> StdioMcpClient.callTool
```

`JsonRpcMcpClient` owns the MCP methods and delegates wire-level communication to a transport strategy:

- `StdioMcpTransport`: MCP JSON-RPC framing over process stdio
- `StreamableHttpMcpTransport`: MCP Streamable HTTP using OkHttp

Both transports support:

- `initialize`
- `notifications/initialized`
- `tools/list`
- `tools/call`

Calls on one `StdioMcpClient` are synchronized. This keeps one server connection safe even though `ToolRegistry` can execute multiple tools concurrently.

MCP tools report `ToolPermission.EXTERNAL`, so `HitlToolRegistry` asks for user approval before executing them.

## Boundaries

- MCP does not change memory policy.
- MCP does not automatically save long-term memory.
- MCP does not automatically search code RAG.
- MCP tools should be treated as potentially external side-effecting tools.
- If dangerous MCP tools are added, this project will need an approval/HITL layer before broad use.
