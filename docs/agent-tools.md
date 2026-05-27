# Tools

## Registry Rule

All tool execution must go through `ToolRegistry`.

Do not call tool implementations directly from `Agent`, `Planner`, or `PlanExecuteAgent`.

## Current Tools

Registered in `Main`:

- `read_file`
- `write_file`
- `list_dir`
- `execute_command`
- `save_memory`
- `search_code`
- `index_code`

Each tool owns:

- name
- description
- JSON schema
- execution behavior

## Execution Behavior

`Agent` passes tool definitions to the LLM on every chat call:

```java
llmClient.chat(conversationHistory, toolRegistry.getToolDefinitions())
```

When tool calls come back:

```text
ToolRegistry.executeTools
  -> submit one Callable per tool call
  -> execute calls concurrently with a fixed thread pool
  -> collect Future results in original tool-call order
  -> return ordered ToolExecutionResult values
```

Unknown tool names and tool exceptions are converted into failed `ToolResult` values. They are returned to the model instead of being swallowed.

## Memory Interaction

After each tool result:

```text
raw tool result -> active conversationHistory tool message
truncated result -> short-term memory TOOL_RESULT
```

This means the current ReAct loop can still reason from the full observation, while session memory keeps a bounded version.

`save_memory` is the exception that intentionally writes long-term memory. It should be called only when the user explicitly asks the CLI to remember a stable fact, preference, or project convention.

`search_code` and `index_code` are RAG tools. `search_code` reads from the SQLite-backed code index; `index_code` rebuilds that index for the current workspace.

## Adding A Tool

When adding a tool:

1. Implement `Tool`.
2. Provide a clear schema in `definition()`.
3. Return structured success or failure through `ToolResult`.
4. Register it in `Main`.
5. Add focused tests for success, failure, and bad input where relevant.

Keep tools deterministic and local-first. This MVP does not include approval flow, browser integration, MCP, or remote tool orchestration.

When multiple tool calls may touch the same file or external resource, the caller/tool design is responsible for avoiding conflicting side effects. `ToolRegistry` preserves result order, not resource-level serialization.
