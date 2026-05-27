package com.example.miniagent.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodebaseRagServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldIndexCodeChunksIntoSqliteAndRetrieveWithHybridSignals() throws IOException {
        Path source = tempDir.resolve("src/main/java/example/MemoryManager.java");
        Path test = tempDir.resolve("src/test/java/example/MemoryManagerTest.java");
        Files.createDirectories(source.getParent());
        Files.createDirectories(test.getParent());
        Files.writeString(source, """
                class MemoryManager {
                    void compressShortTermMemory() {
                        // map reduce summary for conversation memory
                    }
                }
                """);
        Files.writeString(test, """
                class MemoryManagerTest {
                    void shouldTestCompression() {
                        // test compression behavior
                    }
                }
                """);

        CodebaseRagService service = service(tempDir.resolve("rag.sqlite"));
        int chunks = service.index(tempDir);
        List<RagSearchResult> results = service.search("测试 memory compression", 3);

        assertEquals(2, chunks);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result -> result.chunk().codeType() == CodeType.TEST));
        assertTrue(service.buildContext("memory compression", 2).contains("Relevant codebase context"));
    }

    @Test
    void shouldLimitResultsFromSameFile() throws IOException {
        Path source = tempDir.resolve("src/main/java/example/LargeService.java");
        Files.createDirectories(source.getParent());
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            content.append("void memoryCompression").append(i).append("() {}\n");
        }
        Files.writeString(source, content.toString());

        CodebaseRagService service = service(tempDir.resolve("rag.sqlite"));
        service.index(tempDir);
        List<RagSearchResult> results = service.search("memory compression", 5);

        long sameFileCount = results.stream().filter(result -> result.chunk().path().endsWith("LargeService.java")).count();
        assertTrue(sameFileCount <= 2);
    }

    private CodebaseRagService service(Path dbPath) {
        JiebaTokenizer tokenizer = new JiebaTokenizer();
        LocalHashEmbeddingModel embeddingModel = new LocalHashEmbeddingModel(tokenizer);
        return new CodebaseRagService(
                new CodebaseIndexer(embeddingModel),
                new CodebaseRagStore(dbPath, new VectorJsonCodec()),
                new HybridCodeRetriever(embeddingModel, tokenizer)
        );
    }
}
