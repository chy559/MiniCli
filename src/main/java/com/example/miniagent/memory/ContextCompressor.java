package com.example.miniagent.memory;

import com.example.miniagent.llm.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextCompressor {
    private static final int SHORT_TERM_MAP_CHUNK_SIZE = 4;

    public MemoryEntry compressShortTermMemory(List<MemoryEntry> entries) {
        List<String> mappedSummaries = mapShortTermMemory(entries);
        String reducedSummary = reduceShortTermMemory(mappedSummaries);
        return new MemoryEntry(reducedSummary, MemoryType.SUMMARY, Map.of("source", "short-term", "strategy", "map-reduce"), estimateTokens(reducedSummary), System.currentTimeMillis());
    }

    private List<String> mapShortTermMemory(List<MemoryEntry> entries) {
        List<String> summaries = new ArrayList<>();
        for (int start = 0; start < entries.size(); start += SHORT_TERM_MAP_CHUNK_SIZE) {
            int end = Math.min(start + SHORT_TERM_MAP_CHUNK_SIZE, entries.size());
            summaries.add(summarizeChunk(entries.subList(start, end), summaries.size() + 1));
        }
        return summaries;
    }

    private String summarizeChunk(List<MemoryEntry> chunk, int chunkNumber) {
        StringBuilder builder = new StringBuilder("Chunk ").append(chunkNumber).append(": ");
        for (MemoryEntry entry : chunk) {
            builder.append(describeEntry(entry)).append(" ");
        }
        return truncate(builder.toString().trim(), 500);
    }

    private String reduceShortTermMemory(List<String> mappedSummaries) {
        StringBuilder builder = new StringBuilder("Compressed short-term memory (map-reduce): ");
        for (String mappedSummary : mappedSummaries) {
            if (builder.length() > 1200) {
                break;
            }
            builder.append(mappedSummary).append(" ");
        }
        return builder.toString().trim();
    }

    private String describeEntry(MemoryEntry entry) {
        String label = switch (entry.getType()) {
            case CONVERSATION -> entry.getMetadata().getOrDefault("role", "conversation");
            case TOOL_RESULT -> "tool:" + entry.getMetadata().getOrDefault("tool", "unknown");
            case FACT -> "fact";
            case SUMMARY -> "summary";
        };
        return "[" + label + "] " + truncate(entry.getContent(), 160);
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
