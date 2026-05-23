package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolDefinition;

import java.util.Map;

public interface Tool {
    String name();

    String description();

    Map<String, Object> inputSchema();

    ToolResult execute(Map<String, Object> args);

    default ToolDefinition definition() {
        return new ToolDefinition(name(), description(), inputSchema());
    }
}
