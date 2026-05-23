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
    private final int retainRecentRounds;

    public MemoryManager(ConversationMemory conversationMemory,
                         LongTermMemoryStore longTermMemoryStore,
                         MemoryRetriever memoryRetriever,
                         ContextCompressor contextCompressor,
                         int shortTermTokenBudget,
                         int historyTokenBudget) {
        this(conversationMemory, longTermMemoryStore, memoryRetriever, contextCompressor, shortTermTokenBudget, historyTokenBudget, 2);
    }

    public MemoryManager(ConversationMemory conversationMemory,
                         LongTermMemoryStore longTermMemoryStore,
                         MemoryRetriever memoryRetriever,
                         ContextCompressor contextCompressor,
                         int shortTermTokenBudget,
                         int historyTokenBudget,
                         int retainRecentRounds) {
        this.conversationMemory = conversationMemory;
        this.longTermMemoryStore = longTermMemoryStore;
        this.memoryRetriever = memoryRetriever;
        this.contextCompressor = contextCompressor;
        this.shortTermTokenBudget = shortTermTokenBudget;
        this.historyTokenBudget = historyTokenBudget;
        this.retainRecentRounds = Math.max(0, retainRecentRounds);
    }

    public synchronized void addUserMessage(String userInput) {
        conversationMemory.add(new MemoryEntry(userInput, MemoryType.CONVERSATION, Map.of("role", "user"), estimateTokens(userInput), System.currentTimeMillis()));
        compressShortTermMemoryIfNeeded();
    }

    public synchronized void addAssistantMessage(String assistantOutput) {
        conversationMemory.add(new MemoryEntry(assistantOutput, MemoryType.CONVERSATION, Map.of("role", "assistant"), estimateTokens(assistantOutput), System.currentTimeMillis()));
        compressShortTermMemoryIfNeeded();
    }

    public synchronized void addToolResult(String toolName, String toolContent) {
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

    public synchronized String describeStatus() {
        return """
                Memory status
                - short-term entries: %d
                - long-term entries: %d
                - short-term token estimate: %d
                - short-term compression threshold: %d
                - history token budget: %d
                - retain recent rounds: %d
                """.formatted(
                conversationMemory.entries().size(),
                longTermMemoryStore.loadAll().size(),
                conversationMemory.totalTokenEstimate(),
                shortTermTokenBudget,
                historyTokenBudget,
                retainRecentRounds
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
        int retainFromIndex = findRetainFromIndex(entries);
        if (retainFromIndex <= 0) {
            return;
        }

        List<MemoryEntry> older = new java.util.ArrayList<>(entries.subList(0, retainFromIndex));
        List<MemoryEntry> recent = new java.util.ArrayList<>(entries.subList(retainFromIndex, entries.size()));
        MemoryEntry summary = contextCompressor.compressShortTermMemory(older);
        conversationMemory.replaceAll(new java.util.ArrayList<>(List.of(summary)));
        recent.forEach(conversationMemory::add);
    }

    private int findRetainFromIndex(List<MemoryEntry> entries) {
        if (retainRecentRounds == 0) {
            return entries.size();
        }

        int roundsSeen = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (isUserConversation(entries.get(i))) {
                roundsSeen++;
                if (roundsSeen == retainRecentRounds) {
                    return i;
                }
            }
        }
        return 0;
    }

    private boolean isUserConversation(MemoryEntry entry) {
        return entry.getType() == MemoryType.CONVERSATION && "user".equals(entry.getMetadata().get("role"));
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
