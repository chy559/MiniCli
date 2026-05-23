package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;
import com.example.miniagent.llm.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ToolRegistry {
    private final Map<String, Tool> tools = new HashMap<>();

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public List<ToolDefinition> getToolDefinitions() {
        return tools.values().stream().map(Tool::definition).toList();
    }

    public List<ToolExecutionResult> executeTools(List<ToolCall> toolCalls) {
        if (toolCalls.isEmpty()) {
            return List.of();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(toolCalls.size());
        try {
            List<Future<ToolExecutionResult>> futures = new ArrayList<>();
            for (ToolCall toolCall : toolCalls) {
                futures.add(executorService.submit(executeTool(toolCall)));
            }

            List<ToolExecutionResult> results = new ArrayList<>();
            for (int i = 0; i < futures.size(); i++) {
                ToolCall toolCall = toolCalls.get(i);
                Future<ToolExecutionResult> future = futures.get(i);
                try {
                    results.add(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    results.add(new ToolExecutionResult(toolCall.id(), toolCall.name(), new ToolResult(toolCall.name(), "Tool execution interrupted", false)));
                } catch (ExecutionException e) {
                    results.add(new ToolExecutionResult(toolCall.id(), toolCall.name(), new ToolResult(toolCall.name(), "Tool execution failed: " + e.getCause().getMessage(), false)));
                }
            }
            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<ToolExecutionResult> executeTool(ToolCall toolCall) {
        return () -> {
            Tool tool = tools.get(toolCall.name());
            if (tool == null) {
                return new ToolExecutionResult(toolCall.id(), toolCall.name(), new ToolResult(toolCall.name(), "Unknown tool: " + toolCall.name(), false));
            }
            try {
                ToolResult result = tool.execute(toolCall.arguments());
                return new ToolExecutionResult(toolCall.id(), toolCall.name(), result);
            } catch (Exception e) {
                return new ToolExecutionResult(toolCall.id(), toolCall.name(), new ToolResult(toolCall.name(), "Tool execution failed: " + e.getMessage(), false));
            }
        };
    }
}
