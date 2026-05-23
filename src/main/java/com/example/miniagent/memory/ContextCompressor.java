package com.example.miniagent.memory;

import com.example.miniagent.llm.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextCompressor {

    public MemoryEntry compressShortTermMemory(List<MemoryEntry> entries) {
        StringBuilder builder = new StringBuilder("Compressed summary: ");
        for (MemoryEntry entry : entries) {
            if (builder.length() > 600) {
                break;
            }
            builder.append("[").append(entry.getType()).append("] ")
                    .append(truncate(entry.getContent(), 120))
                    .append(" ");
        }
        return new MemoryEntry(builder.toString().trim(), MemoryType.SUMMARY, Map.of("source", "short-term"), estimateTokens(builder.toString()), System.currentTimeMillis());
    }

    public ChatMessage compactConversationHistory(List<ChatMessage> messages) {
        StringBuilder builder = new StringBuilder("Conversation summary: ");
        for (ChatMessage message : messages) {
            if (builder.length() > 800) {
                break;
            }
            builder.append(message.role().name()).append(": ").append(truncate(message.content(), 140)).append(" ");
        }
        return ChatMessage.system(builder.toString().trim());
    }

    public int estimateTokens(String text) {
        return Math.max(1, text == null ? 0 : text.length() / 4);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
