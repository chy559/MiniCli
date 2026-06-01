package com.example.miniagent.tool;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GrepTool implements Tool {
    private final WorkspacePathResolver pathResolver;

    public GrepTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public String name() {
        return "grep";
    }

    @Override
    public String description() {
        return "Search text in workspace files with a regex or literal pattern. Prefer this for symbols, strings, and exact code evidence before semantic RAG.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "pattern", Map.of("type", "string", "description", "Regex pattern by default, or literal text when regex=false."),
                        "path", Map.of("type", "string", "description", "File or directory to search. Defaults to workspace root."),
                        "glob", Map.of("type", "string", "description", "Optional file glob filter, for example **/*.java."),
                        "regex", Map.of("type", "boolean", "description", "Whether pattern is regex. Defaults to true."),
                        "caseSensitive", Map.of("type", "boolean", "description", "Whether matching is case-sensitive. Defaults to true."),
                        "maxResults", Map.of("type", "integer", "description", "Maximum number of matching lines to return. Defaults to 100.")
                ),
                "required", java.util.List.of("pattern")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String rawPattern = String.valueOf(args.get("pattern"));
        if (rawPattern.isBlank() || "null".equalsIgnoreCase(rawPattern)) {
            return new ToolResult(name(), "No grep pattern was provided.", false);
        }
        int maxResults = parseMaxResults(args.get("maxResults"));
        try {
            Pattern pattern = compilePattern(rawPattern, parseBoolean(args.get("regex"), true), parseBoolean(args.get("caseSensitive"), true));
            Path root = pathResolver.resolveInsideWorkspace(String.valueOf(args.getOrDefault("path", ".")));
            PathMatcher globMatcher = buildGlobMatcher(args.get("glob"));
            List<String> matches = new ArrayList<>();

            if (Files.isRegularFile(root)) {
                searchFile(root, pattern, matches, maxResults);
            } else {
                try (var stream = Files.walk(root)) {
                    for (Path file : stream.filter(Files::isRegularFile).toList()) {
                        if (globMatcher == null || globMatcher.matches(Path.of(pathResolver.relativize(file)))) {
                            searchFile(file, pattern, matches, maxResults);
                            if (matches.size() >= maxResults) {
                                break;
                            }
                        }
                    }
                }
            }

            return new ToolResult(name(), matches.isEmpty() ? "No matches found." : String.join(System.lineSeparator(), matches), true);
        } catch (PatternSyntaxException e) {
            return new ToolResult(name(), "Invalid regex pattern: " + e.getMessage(), false);
        } catch (IOException | RuntimeException e) {
            return new ToolResult(name(), "Grep failed: " + e.getMessage(), false);
        }
    }

    @Override
    public ToolPermission permission() {
        return ToolPermission.READ_ONLY;
    }

    private void searchFile(Path file, Pattern pattern, List<String> matches, int maxResults) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size() && matches.size() < maxResults; i++) {
                String line = lines.get(i);
                if (pattern.matcher(line).find()) {
                    matches.add("%s:%d:%s".formatted(pathResolver.relativize(file), i + 1, line.strip()));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private Pattern compilePattern(String rawPattern, boolean regex, boolean caseSensitive) {
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        return Pattern.compile(regex ? rawPattern : Pattern.quote(rawPattern), flags);
    }

    private PathMatcher buildGlobMatcher(Object value) {
        if (value == null || String.valueOf(value).isBlank() || "null".equalsIgnoreCase(String.valueOf(value))) {
            return null;
        }
        return FileSystems.getDefault().getPathMatcher("glob:" + String.valueOf(value).replace('\\', '/'));
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
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
