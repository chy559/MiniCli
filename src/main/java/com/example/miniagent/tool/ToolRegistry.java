package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;
import com.example.miniagent.llm.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public List<ToolDefinition> getToolDefinitions() {
        return tools.values().stream().map(Tool::definition).toList();
    }

    public List<ToolExecutionResult> executeTools(List<ToolCall> toolCalls) {
        List<ToolExecutionResult> results = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            Tool tool = tools.get(toolCall.name());
            if (tool == null) {
                results.add(new ToolExecutionResult(toolCall.id(), toolCall.name(), new ToolResult(toolCall.name(), "Unknown tool: " + toolCall.name(), false)));
                continue;
            }
            try {
                ToolResult result = tool.execute(toolCall.arguments());
                results.add(new ToolExecutionResult(toolCall.id(), toolCall.name(), result));
            } catch (Exception e) {
                results.add(new ToolExecutionResult(toolCall.id(), toolCall.name(), new ToolResult(toolCall.name(), "Tool execution failed: " + e.getMessage(), false)));
            }
        }
        return results;
    }
}
