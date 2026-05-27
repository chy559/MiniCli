package com.example.miniagent.tool;

import com.example.miniagent.rag.CodebaseIndexer;
import com.example.miniagent.rag.CodebaseRagService;
import com.example.miniagent.rag.CodebaseRagStore;
import com.example.miniagent.rag.HybridCodeRetriever;
import com.example.miniagent.rag.JiebaTokenizer;
import com.example.miniagent.rag.LocalHashEmbeddingModel;
import com.example.miniagent.rag.VectorJsonCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRagToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIndexAndSearchCodebaseThroughTools() throws IOException {
        Path source = tempDir.resolve("src/main/java/example/RagMemory.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                class RagMemory {
                    void searchCode() {
                        // hybrid retrieval with sqlite vectors
                    }
                }
                """);
        CodebaseRagService service = ragService(tempDir.resolve("rag.sqlite"));
        IndexCodeTool indexCodeTool = new IndexCodeTool(service, tempDir);
        SearchCodeTool searchCodeTool = new SearchCodeTool(service);

        ToolResult indexResult = indexCodeTool.execute(Map.of());
        ToolResult searchResult = searchCodeTool.execute(Map.of("query", "hybrid retrieval sqlite", "topK", 3));

        assertTrue(indexResult.success());
        assertTrue(indexResult.content().contains("indexed chunks"));
        assertTrue(searchResult.success());
        assertTrue(searchResult.content().contains("RagMemory.java"));
    }

    private CodebaseRagService ragService(Path dbPath) {
        JiebaTokenizer tokenizer = new JiebaTokenizer();
        LocalHashEmbeddingModel embeddingModel = new LocalHashEmbeddingModel(tokenizer);
        return new CodebaseRagService(
                new CodebaseIndexer(embeddingModel),
                new CodebaseRagStore(dbPath, new VectorJsonCodec()),
                new HybridCodeRetriever(embeddingModel, tokenizer)
        );
    }
}
