package com.example.miniagent.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LongTermMemoryStore {
    private final Path filePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LongTermMemoryStore(Path filePath) {
        this.filePath = filePath;
    }

    public synchronized List<LongTermMemoryFact> loadAll() {
        try {
            if (Files.notExists(filePath)) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(Files.readString(filePath), new TypeReference<>() {
            });
        } catch (Exception e) {
            backupCorruptedFile();
            return new ArrayList<>();
        }
    }

    public synchronized void save(LongTermMemoryFact fact) {
        List<LongTermMemoryFact> facts = loadAll();
        facts.add(fact);
        writeAll(facts);
    }

    public synchronized void clear() {
        writeAll(new ArrayList<>());
    }

    private void writeAll(List<LongTermMemoryFact> facts) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), facts);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write long-term memory file", e);
        }
    }

    private void backupCorruptedFile() {
        try {
            if (Files.exists(filePath)) {
                Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".bak");
                Files.copy(filePath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }
}
