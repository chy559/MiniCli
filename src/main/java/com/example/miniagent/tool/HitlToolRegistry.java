package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;

import java.util.ArrayList;
import java.util.List;

public class HitlToolRegistry extends ToolRegistry {
    private final ToolApprovalHandler approvalHandler;

    public HitlToolRegistry(ToolApprovalHandler approvalHandler) {
        this.approvalHandler = approvalHandler;
    }

    @Override
    public List<ToolExecutionResult> executeTools(List<ToolCall> toolCalls) {
        if (toolCalls.isEmpty()) {
            return List.of();
        }

        List<ToolExecutionResult> orderedResults = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            orderedResults.add(null);
        }

        List<ToolCall> approvedCalls = new ArrayList<>();
        List<Integer> approvedIndexes = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            ToolCall toolCall = toolCalls.get(i);
            Tool tool = findTool(toolCall.name());
            if (tool == null || tool.permission() == ToolPermission.READ_ONLY || approvalHandler.approve(toolCall, tool)) {
                approvedCalls.add(toolCall);
                approvedIndexes.add(i);
                continue;
            }

            orderedResults.set(i, new ToolExecutionResult(
                    toolCall.id(),
                    toolCall.name(),
                    new ToolResult(toolCall.name(), "Tool call denied by user approval layer.", false)
            ));
        }

        List<ToolExecutionResult> approvedResults = super.executeTools(approvedCalls);
        for (int i = 0; i < approvedResults.size(); i++) {
            orderedResults.set(approvedIndexes.get(i), approvedResults.get(i));
        }
        return orderedResults;
    }
}
