package com.example.miniagent.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationMemory {
    private final List<MemoryEntry> entries = new ArrayList<>();

    public void add(MemoryEntry entry) {
        entries.add(entry);
    }

    public List<MemoryEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public int totalTokenEstimate() {
        return entries.stream().mapToInt(MemoryEntry::getTokenEstimate).sum();
    }

    public void replaceAll(List<MemoryEntry> replacement) {
        entries.clear();
        entries.addAll(replacement);
    }
}
