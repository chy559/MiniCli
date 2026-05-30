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

Only `stdio` transport is implemented in this MVP. Environment values like `${GITHUB_TOKEN}` are expanded from the process environment.

## Startup Flow

`Main` loads MCP config after registering local tools:

```text
McpConfigLoader.load
  -> McpToolProvider.loadTools
  -> StdioMcpClient.initialize
  -> StdioMcpClient.listTools
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

`StdioMcpClient` uses MCP JSON-RPC framing over stdio and supports:

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
