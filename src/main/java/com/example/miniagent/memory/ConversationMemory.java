package com.example.miniagent.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationMemory {
    private final List<MemoryEntry> entries = new ArrayList<>();

    public synchronized void add(MemoryEntry entry) {
        entries.add(entry);
    }

    public synchronized List<MemoryEntry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized int totalTokenEstimate() {
        return entries.stream().mapToInt(MemoryEntry::getTokenEstimate).sum();
    }

    public synchronized void replaceAll(List<MemoryEntry> replacement) {
        entries.clear();
        entries.addAll(replacement);
    }
}
