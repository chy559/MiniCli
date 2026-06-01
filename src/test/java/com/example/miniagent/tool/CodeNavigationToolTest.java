package com.example.miniagent.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeNavigationToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFindFilesWithGlob() throws IOException {
        writeSourceFile();
        GlobTool tool = new GlobTool(tempDir);

        ToolResult result = tool.execute(Map.of("pattern", "**/*.java"));

        assertTrue(result.success());
        assertTrue(result.content().contains("src/main/java/example/MemoryAgent.java"));
    }

    @Test
    void shouldSearchFileContentWithGrep() throws IOException {
        writeSourceFile();
        GrepTool tool = new GrepTool(tempDir);

        ToolResult result = tool.execute(Map.of(
                "pattern", "saveMemory",
                "glob", "**/*.java",
                "regex", false
        ));

        assertTrue(result.success());
        assertTrue(result.content().contains("MemoryAgent.java:3"));
        assertTrue(result.content().contains("saveMemory"));
    }

    @Test
    void shouldReadLineRange() throws IOException {
        writeSourceFile();
        ReadTool tool = new ReadTool(tempDir);

        ToolResult result = tool.execute(Map.of(
                "path", "src/main/java/example/MemoryAgent.java",
                "startLine", 2,
                "maxLines", 2
        ));

        assertTrue(result.success());
        assertTrue(result.content().contains("2|     void saveMemory() {"));
        assertTrue(result.content().contains("3|         String key = \"saveMemory\";"));
        assertFalse(result.content().contains("4|"));
    }

    @Test
    void shouldRejectReadOutsideWorkspace() {
        ReadTool tool = new ReadTool(tempDir);

        ToolResult result = tool.execute(Map.of("path", "../outside.txt"));

        assertFalse(result.success());
        assertTrue(result.content().contains("outside workspace"));
    }

    @Test
    void searchCodeDescriptionShouldGuideExactToolsFirst() {
        SearchCodeTool tool = new SearchCodeTool(null);

        assertTrue(tool.description().contains("Fuzzy semantic search"));
        assertTrue(tool.description().contains("Prefer glob, grep, and read"));
    }

    private void writeSourceFile() throws IOException {
        Path source = tempDir.resolve("src/main/java/example/MemoryAgent.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                class MemoryAgent {
                    void saveMemory() {
                        String key = "saveMemory";
                    }
                }
                """);
    }
}
