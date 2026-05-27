package com.example.miniagent.rag;

public record RagSearchResult(CodeChunk chunk, double score, double semanticScore, double tokenScore, double typeBonus) {
}
