package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class WriteFileTool implements Tool {
    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "Write UTF-8 text content to a file path, creating parent directories if needed.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string"),
                        "content", Map.of("type", "string")
                ),
                "required", java.util.List.of("path", "content")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String path = String.valueOf(args.get("path"));
        String content = String.valueOf(args.get("content"));
        try {
            Path target = Path.of(path);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, content);
            return new ToolResult(name(), "Wrote file: " + target, true);
        } catch (IOException e) {
            return new ToolResult(name(), "Failed to write file: " + e.getMessage(), false);
        }
    }
}
