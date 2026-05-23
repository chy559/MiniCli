package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ReadFileTool implements Tool {
    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "Read a UTF-8 text file from disk.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("path", Map.of("type", "string")),
                "required", java.util.List.of("path")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String path = String.valueOf(args.get("path"));
        try {
            return new ToolResult(name(), Files.readString(Path.of(path)), true);
        } catch (IOException e) {
            return new ToolResult(name(), "Failed to read file: " + e.getMessage(), false);
        }
    }
}
