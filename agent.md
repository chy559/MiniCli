# Agent Guide

This file is the entry point for coding agents working on this repository. Read it first, then open only the focused docs you need.

## Project Snapshot

Mini Agent CLI is a Java 17 + Maven local agent MVP. It has three core tracks:

- ReAct execution for normal user input.
- Memory with short-term session memory, explicit long-term memory, retrieval, and compression.
- Codebase RAG with SQLite-backed vector chunks and hybrid retrieval.
- `/plan` execution that asks the LLM for a JSON plan, builds a DAG, then executes tasks in dependency order.

Main package:

```text
src/main/java/com/example/miniagent/
```

## Start Here

- Architecture and module boundaries: [docs/agent-architecture.md](docs/agent-architecture.md)
- Prompt and context flow: [docs/agent-prompt-context.md](docs/agent-prompt-context.md)
- Memory strategy: [docs/agent-memory.md](docs/agent-memory.md)
- Codebase RAG strategy: [docs/agent-rag.md](docs/agent-rag.md)
- Tools and execution behavior: [docs/agent-tools.md](docs/agent-tools.md)
- Testing and maintenance rules: [docs/agent-testing-maintenance.md](docs/agent-testing-maintenance.md)
- Original product design: [docs/design.md](docs/design.md)

## Commands

```powershell
mvn test
mvn exec:java
```

Runtime environment:

```powershell
$env:MINI_AGENT_API_KEY="..."
$env:MINI_AGENT_BASE_URL="https://api.openai.com/v1"
$env:MINI_AGENT_MODEL="gpt-4o-mini"
```

Only `MINI_AGENT_API_KEY` is required. The other two have defaults.

## Working Rules

- Keep `Agent`, `PlanExecuteAgent`, `MemoryManager`, `ToolRegistry`, and prompt assembly responsibilities separate.
- Do not auto-save long-term memory. Long-term memory is explicit through `/save <fact>`.
- The `save_memory` tool may save long-term memory only when the user explicitly asks the CLI to remember a stable fact.
- Do not mix short-term memory compression with LLM conversation history compression; they solve different problems.
- All tool execution must go through `ToolRegistry`.
- Planner output must stay strict JSON and DAG validation must remain fail-fast.
- Prefer focused unit tests for behavior changes. Run `mvn test` before handing off.
- When behavior changes, update this file or the focused `docs/agent-*.md` files so future agents see the current rules.

## Known Current Details

- Short-term memory compression uses a Map-Reduce style heuristic summary in `ContextCompressor`.
- `retainRecentRounds` controls how many recent conversation rounds remain uncompressed in short-term memory.
- Conversation history sent to the LLM is rebuilt per `Agent.run(...)`; it is not the same object as `ConversationMemory`.
- Codebase RAG is exposed through `/rag search`, `search_code`, and `index_code`; normal ReAct prompt construction does not automatically inject code snippets.
- Tool results are truncated before entering short-term memory, but raw tool results are still appended to the active ReAct conversation history.
- Multiple tool calls from one LLM response are executed concurrently with a thread pool and `Future`s; results are returned in the original tool-call order.
- In plan execution, independent DAG tasks in the same ready batch are executed concurrently with a thread pool and `Future`s.
