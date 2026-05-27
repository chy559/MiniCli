package com.example.miniagent.tool;

import com.example.miniagent.rag.CodebaseRagService;

import java.util.Map;

public class SearchCodeTool implements Tool {
    private final CodebaseRagService codebaseRagService;

    public SearchCodeTool(CodebaseRagService codebaseRagService) {
        this.codebaseRagService = codebaseRagService;
    }

    @Override
    public String name() {
        return "search_code";
    }

    @Override
    public String description() {
        return "Search the indexed codebase RAG store for relevant files or code snippets.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "The code, behavior, symbol, module, or implementation detail to search for."
                        ),
                        "topK", Map.of(
                                "type", "integer",
                                "description", "Maximum number of code chunks to return. Defaults to 5."
                        )
                ),
                "required", java.util.List.of("query")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String query = String.valueOf(args.get("query")).trim();
        if (query.isBlank() || "null".equalsIgnoreCase(query)) {
            return new ToolResult(name(), "No search query was provided.", false);
        }
        int topK = parseTopK(args.get("topK"));
        String context = codebaseRagService.buildContext(query, topK);
        if (context.isBlank()) {
            return new ToolResult(name(), "No indexed code results found. Run index_code first if the index is empty or stale.", true);
        }
        return new ToolResult(name(), context, true);
    }

    private int parseTopK(Object value) {
        if (value instanceof Number number) {
            return clamp(number.intValue());
        }
        if (value != null) {
            try {
                return clamp(Integer.parseInt(String.valueOf(value)));
            } catch (NumberFormatException ignored) {
            }
        }
        return 5;
    }

    private int clamp(int value) {
        return Math.max(1, Math.min(10, value));
    }
}
