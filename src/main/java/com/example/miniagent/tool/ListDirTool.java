package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class ListDirTool implements Tool {
    @Override
    public String name() {
        return "list_dir";
    }

    @Override
    public String description() {
        return "List files and directories in a path.";
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
        try (var stream = Files.list(Path.of(path))) {
            String content = stream
                    .map(p -> (Files.isDirectory(p) ? "[DIR] " : "[FILE] ") + p.getFileName())
                    .sorted()
                    .collect(Collectors.joining(System.lineSeparator()));
            return new ToolResult(name(), content, true);
        } catch (IOException e) {
            return new ToolResult(name(), "Failed to list directory: " + e.getMessage(), false);
        }
    }
}
