package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ReadTool implements Tool {
    private final WorkspacePathResolver pathResolver;

    public ReadTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public String name() {
        return "read";
    }

    @Override
    public String description() {
        return "Read a workspace file, optionally by line range. Prefer this after glob or grep to inspect exact code before using semantic RAG.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "path", Map.of("type", "string", "description", "Workspace-relative file path."),
                        "startLine", Map.of("type", "integer", "description", "1-based first line to read. Defaults to 1."),
                        "maxLines", Map.of("type", "integer", "description", "Maximum number of lines to return. Defaults to 200.")
                ),
                "required", java.util.List.of("path")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            Path file = pathResolver.resolveInsideWorkspace(String.valueOf(args.get("path")));
            if (!Files.isRegularFile(file)) {
                return new ToolResult(name(), "Path is not a file: " + pathResolver.relativize(file), false);
            }
            List<String> lines = Files.readAllLines(file);
            int startLine = parseInt(args.get("startLine"), 1);
            int maxLines = parseInt(args.get("maxLines"), 200);
            int startIndex = Math.max(0, startLine - 1);
            int endIndex = Math.min(lines.size(), startIndex + Math.max(1, Math.min(maxLines, 1_000)));
            if (startIndex >= lines.size()) {
                return new ToolResult(name(), "Start line is beyond end of file. Total lines: " + lines.size(), true);
            }

            StringBuilder builder = new StringBuilder();
            for (int i = startIndex; i < endIndex; i++) {
                builder.append("%d| %s".formatted(i + 1, lines.get(i))).append(System.lineSeparator());
            }
            return new ToolResult(name(), builder.toString().stripTrailing(), true);
        } catch (IOException | RuntimeException e) {
            return new ToolResult(name(), "Read failed: " + e.getMessage(), false);
        }
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ_ONLY;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
