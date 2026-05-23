package com.example.miniagent.llm;

public record ChatMessage(Role role, String content, String name, String toolCallId) {

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content, null, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content, null, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content, null, null);
    }

    public static ChatMessage tool(String toolCallId, String name, String content) {
        return new ChatMessage(Role.TOOL, content, name, toolCallId);
    }
}
