package com.example.miniagent.rag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CodebaseRagStore {
    private final Path dbPath;
    private final VectorJsonCodec vectorJsonCodec;

    public CodebaseRagStore(Path dbPath, VectorJsonCodec vectorJsonCodec) {
        this.dbPath = dbPath;
        this.vectorJsonCodec = vectorJsonCodec;
        initialize();
    }

    public synchronized void replaceAll(List<CodeChunk> chunks) {
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.executeUpdate("DELETE FROM code_chunks");
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO code_chunks(path, chunk_index, content, code_type, vector_json, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                for (CodeChunk chunk : chunks) {
                    insert.setString(1, chunk.path());
                    insert.setInt(2, chunk.chunkIndex());
                    insert.setString(3, chunk.content());
                    insert.setString(4, chunk.codeType().name());
                    insert.setString(5, vectorJsonCodec.encode(chunk.vector()));
                    insert.setLong(6, System.currentTimeMillis());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to replace code RAG index", e);
        }
    }

    public synchronized List<CodeChunk> loadAll() {
        List<CodeChunk> chunks = new ArrayList<>();
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("SELECT path, chunk_index, content, code_type, vector_json FROM code_chunks ORDER BY path, chunk_index");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                chunks.add(new CodeChunk(
                        resultSet.getString("path"),
                        resultSet.getInt("chunk_index"),
                        resultSet.getString("content"),
                        CodeType.valueOf(resultSet.getString("code_type")),
                        vectorJsonCodec.decode(resultSet.getString("vector_json"))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load code RAG index", e);
        }
        return chunks;
    }

    public synchronized int count() {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM code_chunks")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count code RAG chunks", e);
        }
    }

    private void initialize() {
        createParentDirectory();
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS code_chunks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        path TEXT NOT NULL,
                        chunk_index INTEGER NOT NULL,
                        content TEXT NOT NULL,
                        code_type TEXT NOT NULL,
                        vector_json TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_code_chunks_path ON code_chunks(path)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize code RAG store", e);
        }
    }

    private void createParentDirectory() {
        Path parent = dbPath.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create code RAG store directory", e);
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }
}
