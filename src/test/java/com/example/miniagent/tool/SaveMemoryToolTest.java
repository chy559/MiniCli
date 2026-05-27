package com.example.miniagent.tool;

import com.example.miniagent.memory.ContextCompressor;
import com.example.miniagent.memory.ConversationMemory;
import com.example.miniagent.memory.LongTermMemoryStore;
import com.example.miniagent.memory.MemoryManager;
import com.example.miniagent.memory.MemoryRetriever;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveMemoryToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistFactIntoLongTermMemory() {
        MemoryManager memoryManager = memoryManager();
        SaveMemoryTool tool = new SaveMemoryTool(memoryManager);

        ToolResult result = tool.execute(Map.of("fact", "User prefers concise Chinese explanations"));

        assertTrue(result.success());
        assertTrue(memoryManager.buildContextForQuery("Chinese explanations", 3).contains("concise Chinese"));
    }

    private MemoryManager memoryManager() {
        return new MemoryManager(
                new ConversationMemory(),
                new LongTermMemoryStore(tempDir.resolve("memory.json")),
                new MemoryRetriever(),
                new ContextCompressor(),
                200,
                500
        );
    }
}
