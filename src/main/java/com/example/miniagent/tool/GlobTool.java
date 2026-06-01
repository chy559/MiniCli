package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobTool implements Tool {
    private final WorkspacePathResolver pathResolver;

    public GlobTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public String name() {
        return "glob";
    }

    @Override
    public String description() {
        return "Find files by glob pattern inside the workspace. Prefer this for exact codebase discovery before semantic RAG.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pattern", Map.of("type", "string", "description", "Glob pattern, for example **/*.java or src/**/Memory*.java."),
                        "path", Map.of("type", "string", "description", "Directory to search from. Defaults to workspace root."),
                        "maxResults", Map.of("type", "integer", "description", "Maximum number of file paths to return. Defaults to 100.")
                ),
                "required", java.util.List.of("pattern")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String pattern = String.valueOf(args.get("pattern")).trim();
        if (pattern.isBlank() || "null".equalsIgnoreCase(pattern)) {
            return new ToolResult(name(), "No glob pattern was provided.", false);
        }
        int maxResults = parseMaxResults(args.get("maxResults"));
        try {
            Path root = pathResolver.resolveInsideWorkspace(String.valueOf(args.getOrDefault("path", ".")));
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + normalizeGlob(pattern));
            try (var stream = Files.walk(root)) {
                String content = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> matcher.matches(Path.of(pathResolver.relativize(path))))
                        .limit(maxResults)
                        .map(pathResolver::relativize)
                        .sorted()
                        .collect(Collectors.joining(System.lineSeparator()));
                return new ToolResult(name(), content.isBlank() ? "No files matched." : content, true);
            }
        } catch (IOException | RuntimeException e) {
            return new ToolResult(name(), "Glob failed: " + e.getMessage(), false);
        }
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ_ONLY;
    }

    private String normalizeGlob(String pattern) {
        return pattern.replace('\\', '/');
    }

    private int parseMaxResults(Object value) {
        if (value instanceof Number number) {
            return clamp(number.intValue());
        }
        if (value != null) {
            try {
                return clamp(Integer.parseInt(String.valueOf(value)));
            } catch (NumberFormatException ignored) {
            }
        }
        return 100;
    }

    private int clamp(int value) {
        return Math.max(1, Math.min(500, value));
    }
}
