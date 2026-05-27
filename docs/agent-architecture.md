# Agent Architecture

## Goal

This repository is intentionally small. Preserve clear module ownership over adding broad abstractions.

## Runtime Flow

Normal CLI input:

```text
Main
  -> CommandParser
  -> Agent.run
  -> MemoryManager.addUserMessage
  -> MemoryManager.buildContextForQuery
  -> PromptAssembler.assembleReactPrompt
  -> LlmClient.chat(messages, tools)
  -> ToolRegistry.executeTools if needed
  -> MemoryManager.addToolResult / addAssistantMessage
```

Plan input:

```text
Main
  -> PlanExecuteAgent.run
  -> Planner.createPlan
  -> Planner.parsePlan
  -> topological sort
  -> execute each ready DAG batch concurrently
  -> Agent.runWithInstruction for each task
```

## Module Responsibilities

`cli`

- Owns interactive input and slash command routing.
- Wires runtime dependencies in `Main`.

`agent`

- `Agent` owns the ReAct loop only.
- `PlanExecuteAgent` owns plan execution only and delegates task execution to `Agent`.
- `PlanExecuteAgent` executes all currently ready DAG nodes in a concurrent batch, then waits for their `Future`s before moving to dependent nodes.

`plan`

- `Planner` asks the model for strict JSON, parses it, validates dependencies, and topologically sorts tasks.
- `ExecutionPlan` and `Task` are state models for DAG execution.

`memory`

- `MemoryManager` is the facade for short-term memory, long-term memory, retrieval, and compression.
- `ConversationMemory` is in-process short-term state.
- `LongTermMemoryStore` persists facts to JSON.
- `MemoryRetriever` performs keyword retrieval.
- `ContextCompressor` performs heuristic compression.

`rag`

- `CodebaseRagService` is the facade for indexing, searching, status, and prompt context formatting.
- `CodebaseRagStore` persists chunks and JSON vectors in SQLite.
- `CodebaseIndexer` walks and chunks repository files.
- `HybridCodeRetriever` performs cosine baseline retrieval, token weighting, code type bonus, and same-file limiting.

`prompt`

- `PromptRepository` stores prompt text.
- `PromptAssembler` combines base prompts, relevant memory context, and optional task instructions.

`tool`

- `ToolRegistry` is the only execution gateway.
- Individual tools define schemas and execution behavior.

`llm`

- Contains chat message, response, tool-call models, and the OpenAI-compatible client.

## Boundaries To Preserve

- Do not let `Agent` parse slash commands.
- Do not let `Planner` execute tools.
- Do not let tool implementations write directly into memory.
- Do not move prompt construction into `Agent` unless there is a strong reason.
- Do not add persistence to short-term memory unless the product goal changes.
- If execution behavior changes, update the relevant agent docs in this directory.
