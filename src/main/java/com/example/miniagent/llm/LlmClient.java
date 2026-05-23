package com.example.miniagent.llm;

import java.util.List;

public interface LlmClient {
    ChatResponse chat(List<ChatMessage> conversationHistory, List<ToolDefinition> toolDefinitions);
}
