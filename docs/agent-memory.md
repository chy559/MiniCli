# Memory Strategy

## Layers

The project has two memory layers:

- Short-term memory: in-process `ConversationMemory`, used for the current CLI session.
- Long-term memory: JSON-backed `LongTermMemoryStore`, used for stable facts across sessions.

## Short-Term Memory

Stored entry types:

- `CONVERSATION`: user and assistant messages.
- `TOOL_RESULT`: truncated tool output.
- `SUMMARY`: compressed older short-term entries.
- `FACT`: available in the model but not currently used by the main flow.

Writes happen through `MemoryManager`:

- `addUserMessage`
- `addAssistantMessage`
- `addToolResult`

Tool results are truncated before entering short-term memory. Keep this behavior unless a more robust summarizer is introduced.

Short-term memory and the LLM-facing `conversationHistory` are separate structures. User input is written to short-term memory, but the model mainly receives the active `conversationHistory` built inside `Agent.runWithInstruction(...)`. Short-term memory is used for status, compression, and bounded tool-result recording; it is not automatically replayed into prompts.

## Short-Term Compression

Compression is triggered when:

```text
conversationMemory.totalTokenEstimate() > shortTermTokenBudget
and entry count >= 6
```

Current strategy:

```text
older entries -> Map-Reduce summary -> one SUMMARY entry
recent rounds -> preserved raw
```

`retainRecentRounds` controls how many recent conversation rounds stay out of compression. A round is counted from user messages; when the retain boundary is found, all entries from that user message onward remain raw.

Current default:

```text
retainRecentRounds = 2
```

`ContextCompressor.compressShortTermMemory(...)` implements the heuristic Map-Reduce flow:

```text
Map: split old entries into chunks of 4 and summarize each chunk
Reduce: merge chunk summaries into one compressed short-term memory summary
```

The summary entry metadata includes:

```text
source=short-term
strategy=map-reduce
```

## Long-Term Memory

Long-term memory is explicit. It is saved only through:

```text
/save <fact>
save_memory tool, when the user explicitly asks the CLI to remember a stable fact
```

Default file path:

```text
~/.mini-agent/memory.json
```

Do not auto-save:

- current task goals
- temporary execution steps
- tool results
- ordinary chat turns

The `save_memory` tool does not loosen this rule. It exists so the model can honor natural-language requests such as "remember that I prefer concise Chinese answers" without requiring the user to type `/save`.

Long-term memory should represent stable facts, user preferences, coding conventions, and durable project notes.

## Retrieval

`MemoryRetriever` uses lightweight keyword overlap. There is no embedding store or vector database.

Retrieval flow:

```text
current query
  -> tokenize
  -> score long-term facts by keyword overlap
  -> take top K
  -> inject as "Relevant long-term memory"
```

The current ReAct path asks for top 3 facts.

Long-term memory retrieval is the memory layer that is injected into the ReAct system prompt. This is different from short-term memory storage and different from codebase RAG.

## Important Distinction

Short-term memory compression and conversation history compression are separate:

- Short-term compression changes `ConversationMemory`.
- Conversation history compression changes the active message list sent to the LLM.

Do not merge these flows without a deliberate design change.
