package com.example.miniagent.tool;

import com.example.miniagent.rag.CodebaseRagService;

import java.nio.file.Path;
import java.util.Map;

public class IndexCodeTool implements Tool {
    private final CodebaseRagService codebaseRagService;
    private final Path workspaceRoot;

    public IndexCodeTool(CodebaseRagService codebaseRagService, Path workspaceRoot) {
        this.codebaseRagService = codebaseRagService;
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public String name() {
        return "index_code";
    }

    @Override
    public String description() {
        return "Rebuild the codebase RAG index for the current workspace before searching code.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", java.util.List.of()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        int chunks = codebaseRagService.index(workspaceRoot);
        return new ToolResult(name(), "Code RAG indexed chunks: " + chunks, true);
    }
}
