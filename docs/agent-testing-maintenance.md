# Testing And Maintenance

## Test Command

```powershell
mvn test
```

Run it after behavior changes. For docs-only changes, tests are optional, but mention if they were not run.

## Existing Test Areas

- `AgentTest`: direct answer and tool-loop behavior.
- `CommandParserTest`: slash command parsing.
- `MemoryManagerTest`: persistence, retrieval, compression, clearing memory.
- `PlannerAndExecutorTest`: JSON plan parsing, DAG ordering, cycle detection, task execution order.

## Testing Guidelines

- Prefer stub `LlmClient` implementations over live model calls.
- Test behavior at module boundaries.
- For memory changes, assert both the retained raw entries and the generated summary entry.
- For RAG changes, test SQLite persistence, retrieval scoring behavior, and same-file limiting.
- For planner changes, test malformed JSON, dependency validation, and cycle detection.
- For tools, test failure returns as data rather than thrown exceptions when routed through `ToolRegistry`.
- For concurrency changes, test both concurrent overlap and deterministic externally visible ordering.

## Maintenance Notes

- The repo may show generated `target/` changes after running Maven because build outputs are present locally. Do not treat those as source edits.
- Keep code Java 17 compatible.
- Keep comments sparse and useful.
- Avoid broad refactors when changing one subsystem.
- Preserve existing package boundaries unless the architecture changes intentionally.
- Keep `agent.md` and focused `docs/agent-*.md` files current when behavior or agent-facing rules change.

## Common Pitfalls

- Confusing `ConversationMemory` with the active LLM `conversationHistory`.
- Adding long-term memory automatically from normal conversation.
- Letting `PlanExecuteAgent` solve task logic directly instead of delegating to `Agent`.
- Returning fabricated command output instead of routing through tools.
- Expanding prompt text without updating tests that depend on message construction.
- Adding concurrent execution without checking shared state such as memory writes, result ordering, or task dependency boundaries.
