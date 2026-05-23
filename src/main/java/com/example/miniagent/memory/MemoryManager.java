package com.example.miniagent.memory;

import java.util.List;
import java.util.Map;

public class MemoryManager {
    private final ConversationMemory conversationMemory;
    private final LongTermMemoryStore longTermMemoryStore;
    private final MemoryRetriever memoryRetriever;
    private final ContextCompressor contextCompressor;
    private final int shortTermTokenBudget;
    private final int historyTokenBudget;

    public MemoryManager(ConversationMemory conversationMemory,
                         LongTermMemoryStore longTermMemoryStore,
                         MemoryRetriever memoryRetriever,
                         ContextCompressor contextCompressor,
                         int shortTermTokenBudget,
                         int historyTokenBudget) {
        this.conversationMemory = conversationMemory;
        this.longTermMemoryStore = longTermMemoryStore;
        this.memoryRetriever = memoryRetriever;
        this.contextCompressor = contextCompressor;
        this.shortTermTokenBudget = shortTermTokenBudget;
        this.historyTokenBudget = historyTokenBudget;
    }

    public void addUserMessage(String userInput) {
        conversationMemory.add(new MemoryEntry(userInput, MemoryType.CONVERSATION, Map.of("role", "user"), estimateTokens(userInput), System.currentTimeMillis()));
        compressShortTermMemoryIfNeeded();
    }

    public void addAssistantMessage(String assistantOutput) {
        conversationMemory.add(new MemoryEntry(assistantOutput, MemoryType.CONVERSATION, Map.of("role", "assistant"), estimateTokens(assistantOutput), System.currentTimeMillis()));
        compressShortTermMemoryIfNeeded();
    }

    public void addToolResult(String toolName, String toolContent) {
        String summary = truncate(toolContent, 500);
        conversationMemory.add(new MemoryEntry(summary, MemoryType.TOOL_RESULT, Map.of("tool", toolName), estimateTokens(summary), System.currentTimeMillis()));
        compressShortTermMemoryIfNeeded();
    }

    public String buildContextForQuery(String query, int topK) {
        List<LongTermMemoryFact> relevant = memoryRetriever.retrieveRelevant(query, longTermMemoryStore.loadAll(), topK);
        if (relevant.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Relevant long-term memory:\n");
        for (LongTermMemoryFact fact : relevant) {
            builder.append("- ").append(fact.getContent()).append("\n");
        }
        return builder.toString().trim();
    }

    public void saveLongTermFact(String fact) {
        longTermMemoryStore.save(new LongTermMemoryFact(fact, List.of("user"), System.currentTimeMillis()));
    }

    public void clearLongTermMemory() {
        longTermMemoryStore.clear();
    }

    public String describeStatus() {
        return """
                Memory status
                - short-term entries: %d
                - long-term entries: %d
                - short-term token estimate: %d
                - short-term compression threshold: %d
                - history token budget: %d
                """.formatted(
                conversationMemory.entries().size(),
                longTermMemoryStore.loadAll().size(),
                conversationMemory.totalTokenEstimate(),
                shortTermTokenBudget,
                historyTokenBudget
        ).trim();
    }

    public ConversationMemory conversationMemory() {
        return conversationMemory;
    }

    public ContextCompressor contextCompressor() {
        return contextCompressor;
    }

    public int historyTokenBudget() {
        return historyTokenBudget;
    }

    private void compressShortTermMemoryIfNeeded() {
        if (conversationMemory.totalTokenEstimate() <= shortTermTokenBudget || conversationMemory.entries().size() < 6) {
            return;
        }
        List<MemoryEntry> entries = conversationMemory.entries();
        int keepRecent = 4;
        List<MemoryEntry> older = new java.util.ArrayList<>(entries.subList(0, entries.size() - keepRecent));
        List<MemoryEntry> recent = new java.util.ArrayList<>(entries.subList(entries.size() - keepRecent, entries.size()));
        MemoryEntry summary = contextCompressor.compressShortTermMemory(older);
        conversationMemory.replaceAll(new java.util.ArrayList<>(List.of(summary)));
        recent.forEach(conversationMemory::add);
    }

    private int estimateTokens(String text) {
        return contextCompressor.estimateTokens(text);
    }

    private String truncate(String content, int maxLen) {
        if (content == null) {
            return "";
        }
        return content.length() <= maxLen ? content : content.substring(0, maxLen) + "...";
    }
}
