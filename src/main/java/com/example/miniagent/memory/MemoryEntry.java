package com.example.miniagent.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoryEntry {
    private String id;
    private String content;
    private MemoryType type;
    private Map<String, String> metadata;
    private int tokenEstimate;
    private long createdAt;

    public MemoryEntry() {
    }

    public MemoryEntry(String content, MemoryType type, Map<String, String> metadata, int tokenEstimate, long createdAt) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.type = type;
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        this.tokenEstimate = tokenEstimate;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public MemoryType getType() {
        return type;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public int getTokenEstimate() {
        return tokenEstimate;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
