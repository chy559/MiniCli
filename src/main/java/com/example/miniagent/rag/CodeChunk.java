package com.example.miniagent.rag;

public record CodeChunk(
        String path,
        int chunkIndex,
        String content,
        CodeType codeType,
        double[] vector
) {
}
