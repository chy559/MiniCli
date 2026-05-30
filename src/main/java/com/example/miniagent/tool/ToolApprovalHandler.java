package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;

public interface ToolApprovalHandler {
    boolean approve(ToolCall toolCall, Tool tool);
}
