package com.example.miniagent.tool;

import com.example.miniagent.llm.ToolCall;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

public class ConsoleToolApprovalHandler implements ToolApprovalHandler {
    private final BufferedReader reader;
    private final PrintStream out;

    public ConsoleToolApprovalHandler(BufferedReader reader, PrintStream out) {
        this.reader = reader;
        this.out = out;
    }

    @Override
    public boolean approve(ToolCall toolCall, Tool tool) {
        out.println("Tool call requires approval:");
        out.println("- tool: " + tool.name());
        out.println("- permission: " + tool.permission());
        out.println("- args: " + toolCall.arguments());
        out.print("Allow? [y/N]: ");
        try {
            String answer = reader.readLine();
            return answer != null && ("y".equalsIgnoreCase(answer.trim()) || "yes".equalsIgnoreCase(answer.trim()));
        } catch (IOException e) {
            return false;
        }
    }
}
