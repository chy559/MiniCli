package com.example.miniagent.rag;

import java.nio.file.Path;
import java.util.List;

public class CodebaseRagService {
    private final CodebaseIndexer indexer;
    private final CodebaseRagStore store;
    private final HybridCodeRetriever retriever;

    public CodebaseRagService(CodebaseIndexer indexer, CodebaseRagStore store, HybridCodeRetriever retriever) {
        this.indexer = indexer;
        this.store = store;
        this.retriever = retriever;
    }

    public int index(Path root) {
        List<CodeChunk> chunks = indexer.index(root);
        store.replaceAll(chunks);
        return chunks.size();
    }

    public List<RagSearchResult> search(String query, int topK) {
        return retriever.search(query, store.loadAll(), topK);
    }

    public String buildContext(String query, int topK) {
        List<RagSearchResult> results = search(query, topK);
        if (results.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Relevant codebase context:\n");
        for (RagSearchResult result : results) {
            CodeChunk chunk = result.chunk();
            builder.append("- ")
                    .append(chunk.path())
                    .append("#chunk-")
                    .append(chunk.chunkIndex())
                    .append(" [")
                    .append(chunk.codeType())
                    .append(", score=")
                    .append("%.3f".formatted(result.score()))
                    .append("]\n")
                    .append(truncate(chunk.content(), 900))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    public String describeStatus() {
        return "Code RAG status\n- chunks: %d".formatted(store.count());
    }

    private String truncate(String content, int maxLen) {
        if (content == null) {
            return "";
        }
        return content.length() <= maxLen ? content : content.substring(0, maxLen) + "...";
    }
}
