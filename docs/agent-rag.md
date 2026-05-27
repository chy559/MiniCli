# Codebase RAG

## Purpose

The codebase RAG feature gives the agent lightweight repository awareness without requiring a remote embedding service or a separate vector database.

## Commands

```text
/rag
/rag index
/rag search <query>
```

- `/rag` shows index status.
- `/rag index` scans the current workspace and rebuilds the SQLite index.
- `/rag search <query>` prints top matching chunks and score components.

## Storage

Default database:

```text
~/.mini-agent/code-rag.sqlite
```

SQLite table:

```text
code_chunks(
  id,
  path,
  chunk_index,
  content,
  code_type,
  vector_json,
  created_at
)
```

Vectors are persisted as JSON arrays in `vector_json`.

## Indexing

`CodebaseIndexer` walks the workspace, skips `.git`, `target`, binary artifacts, and SQLite database files, then indexes common code, docs, and configuration extensions.

Files are split into chunks using bounded line and character limits:

```text
max chunk lines: 80
max chunk chars: 4000
```

Each chunk stores:

- relative path
- chunk index
- content
- code type
- local hash embedding vector

## Embeddings And Tokenization

`LocalHashEmbeddingModel` creates deterministic local vectors from tokens. This keeps tests and local runs offline.

`JiebaTokenizer` is the local tokenizer used for Chinese/code-aware retrieval signals:

- Chinese n-grams
- English/code tokens
- snake_case and path separators

## Retrieval

`HybridCodeRetriever` loads chunks from SQLite into memory, then scores them in two stages:

1. Semantic retrieval baseline: cosine similarity between query vector and chunk vector.
2. Hybrid rerank:

```text
score = semantic * 0.70 + jiebaTokenScore * 0.25 + codeTypeBonus
```

Additional rules:

- Code type bonus boosts likely source, test, doc, or config matches based on query words.
- Same-file limit keeps at most 2 chunks from one file in the final result set.

## Prompt Injection

`Agent` asks `CodebaseRagService.buildContext(userInput, 5)` for relevant code context on each run. If results exist, they are appended to the same context block as long-term memory before the ReAct system prompt is sent to the LLM.

Do not confuse codebase RAG with long-term memory:

- RAG is repository content, rebuilt from files.
- Long-term memory is user-saved durable facts.
