package com.example.miniagent.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistAndRetrieveLongTermMemory() {
        MemoryManager manager = new MemoryManager(
                new ConversationMemory(),
                new LongTermMemoryStore(tempDir.resolve("memory.json")),
                new MemoryRetriever(),
                new ContextCompressor(),
                100,
                500
        );

        manager.saveLongTermFact("User prefers Chinese explanations");
        String context = manager.buildContextForQuery("Please answer in Chinese", 3);

        assertTrue(context.contains("Chinese"));
    }

    @Test
    void shouldCompressShortTermMemoryWhenThresholdExceeded() {
        MemoryManager manager = new MemoryManager(
                new ConversationMemory(),
                new LongTermMemoryStore(tempDir.resolve("memory.json")),
                new MemoryRetriever(),
                new ContextCompressor(),
                20,
                500
        );

        for (int i = 0; i < 8; i++) {
            manager.addUserMessage("This is a fairly long message " + i);
        }

        assertTrue(manager.conversationMemory().entries().stream().anyMatch(entry -> entry.getType() == MemoryType.SUMMARY));
    }

    @Test
    void shouldClearLongTermMemory() {
        MemoryManager manager = new MemoryManager(
                new ConversationMemory(),
                new LongTermMemoryStore(tempDir.resolve("memory.json")),
                new MemoryRetriever(),
                new ContextCompressor(),
                100,
                500
        );
        manager.saveLongTermFact("fact");
        manager.clearLongTermMemory();
        assertEquals("", manager.buildContextForQuery("fact", 3));
    }
}
