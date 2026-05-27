# Prompt And Context Flow

## ReAct Prompt Construction

`Agent.run(userInput)` delegates to `runWithInstruction(userInput, null)`.

Initial message construction:

```text
1. system: PromptAssembler.assembleReactPrompt(memoryContext, overrideInstruction)
2. user: current user input
```

The system message is assembled in this order:

```text
base ReAct system prompt

relevant long-term memory, if any

relevant codebase RAG context, if indexed and matching

override instruction, only for plan task execution
```

Important: short-term `ConversationMemory` is not dumped into every LLM call. The active `conversationHistory` is a per-run ReAct message list. Codebase RAG context is retrieved separately from SQLite-backed chunks.

## Tool Loop Context

On each LLM call, `Agent` sends:

```text
conversationHistory
toolRegistry.getToolDefinitions()
```

If the model returns tool calls:

```text
assistant response
tool result message
```

are appended to the active `conversationHistory`, then the loop continues.

If the model returns no tool calls, the assistant content is saved into short-term memory and returned.

## Conversation History Compression

Before every model call, `Agent.maybeCompactConversationHistory(...)` estimates message token usage. If the active history is over budget and has enough messages:

```text
older active messages -> one system summary
latest 4 active messages -> kept raw
```

This protects the model input size for the current ReAct run.

Do not confuse this with short-term memory compression. Conversation history compression affects what is sent to the LLM in the current loop. Short-term memory compression affects `ConversationMemory` storage.

## Plan Task Prompting

`PlanExecuteAgent` executes each DAG task through:

```java
taskExecutorAgent.runWithInstruction(task.getDescription(), overrideInstruction)
```

That means task execution still uses ReAct, tools, memory retrieval, and context compression, but gets an extra instruction telling the model to stay within the current task.

## Prompt Editing Guidelines

- Keep base ReAct prompt focused on behavior: understand goal, use tools when needed, do not fabricate observations.
- Keep planner prompt strict: JSON only, minimal steps, valid dependencies.
- Keep task executor prompt constrained: solve only the current task and return a compact result.
- If prompt behavior changes, add or update tests using stub `LlmClient` implementations where possible.
