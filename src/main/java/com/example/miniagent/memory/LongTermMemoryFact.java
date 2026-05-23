package com.example.miniagent.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LongTermMemoryFact {
    private String id;
    private String content;
    private List<String> tags;
    private long createdAt;

    public LongTermMemoryFact() {
    }

    public LongTermMemoryFact(String content, List<String> tags, long createdAt) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public List<String> getTags() {
        return tags;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
