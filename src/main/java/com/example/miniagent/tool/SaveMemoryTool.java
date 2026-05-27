package com.example.miniagent.tool;

import com.example.miniagent.memory.MemoryManager;

import java.util.Map;

public class SaveMemoryTool implements Tool {
    private final MemoryManager memoryManager;

    public SaveMemoryTool(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String name() {
        return "save_memory";
    }

    @Override
    public String description() {
        return "Save a stable long-term memory fact only when the user explicitly asks the CLI to remember it.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "fact", Map.of(
                                "type", "string",
                                "description", "A stable fact, preference, or project convention the user wants remembered."
                        )
                ),
                "required", java.util.List.of("fact")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        String fact = String.valueOf(args.get("fact")).trim();
        if (fact.isBlank() || "null".equalsIgnoreCase(fact)) {
            return new ToolResult(name(), "No memory fact was provided.", false);
        }
        memoryManager.saveLongTermFact(fact);
        return new ToolResult(name(), "Saved long-term memory: " + fact, true);
    }
}
