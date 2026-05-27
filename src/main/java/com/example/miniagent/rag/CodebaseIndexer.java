package com.example.miniagent.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class CodebaseIndexer {
    private static final int MAX_CHUNK_CHARS = 4_000;
    private static final int MAX_CHUNK_LINES = 80;
    private static final Set<String> INDEXED_EXTENSIONS = Set.of(
            ".java", ".kt", ".go", ".rs", ".py", ".js", ".jsx", ".ts", ".tsx",
            ".md", ".xml", ".json", ".yaml", ".yml", ".toml", ".properties"
    );

    private final LocalHashEmbeddingModel embeddingModel;

    public CodebaseIndexer(LocalHashEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<CodeChunk> index(Path root) {
        List<CodeChunk> chunks = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> shouldIndex(root, path))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .toList();
            for (Path file : files) {
                chunks.addAll(chunkFile(root, file));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to index codebase", e);
        }
        return chunks;
    }

    private List<CodeChunk> chunkFile(Path root, Path file) {
        try {
            List<String> lines = Files.readAllLines(file);
            List<CodeChunk> chunks = new ArrayList<>();
            String relativePath = root.relativize(file).toString().replace('\\', '/');
            CodeType codeType = CodeType.fromPath(file);
            int chunkIndex = 0;
            StringBuilder builder = new StringBuilder();
            int lineCount = 0;

            for (String line : lines) {
                if (builder.length() + line.length() > MAX_CHUNK_CHARS || lineCount >= MAX_CHUNK_LINES) {
                    addChunk(chunks, relativePath, chunkIndex++, builder.toString(), codeType);
                    builder.setLength(0);
                    lineCount = 0;
                }
                builder.append(line).append(System.lineSeparator());
                lineCount++;
            }
            if (!builder.isEmpty()) {
                addChunk(chunks, relativePath, chunkIndex, builder.toString(), codeType);
            }
            return chunks;
        } catch (IOException e) {
            return List.of();
        }
    }

    private void addChunk(List<CodeChunk> chunks, String relativePath, int chunkIndex, String content, CodeType codeType) {
        String embeddingText = relativePath + "\n" + codeType + "\n" + content;
        chunks.add(new CodeChunk(relativePath, chunkIndex, content, codeType, embeddingModel.embed(embeddingText)));
    }

    private boolean shouldIndex(Path root, Path path) {
        String relative = root.relativize(path).toString().replace('\\', '/');
        String lower = relative.toLowerCase();
        if (lower.startsWith(".git/") || lower.startsWith("target/") || lower.contains("/target/")) {
            return false;
        }
        if (lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".class") || lower.endsWith(".jar")) {
            return false;
        }
        return INDEXED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
