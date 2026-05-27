package com.example.miniagent.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HybridCodeRetriever {
    private static final int MAX_RESULTS_PER_FILE = 2;
    private final LocalHashEmbeddingModel embeddingModel;
    private final JiebaTokenizer tokenizer;

    public HybridCodeRetriever(LocalHashEmbeddingModel embeddingModel, JiebaTokenizer tokenizer) {
        this.embeddingModel = embeddingModel;
        this.tokenizer = tokenizer;
    }

    public List<RagSearchResult> search(String query, List<CodeChunk> chunks, int topK) {
        if (query == null || query.isBlank() || chunks.isEmpty() || topK <= 0) {
            return List.of();
        }

        double[] queryVector = embeddingModel.embed(query);
        Set<String> queryTokens = new HashSet<>(tokenizer.tokenize(query));
        int candidateCount = Math.min(chunks.size(), Math.max(topK * 10, 50));
        List<RagSearchResult> semanticCandidates = chunks.stream()
                .map(chunk -> score(query, queryVector, queryTokens, chunk))
                .sorted(Comparator.comparingDouble(RagSearchResult::semanticScore).reversed())
                .limit(candidateCount)
                .sorted(Comparator.comparingDouble(RagSearchResult::score).reversed())
                .toList();

        List<RagSearchResult> limited = new ArrayList<>();
        Map<String, Integer> perFileCounts = new HashMap<>();
        for (RagSearchResult result : semanticCandidates) {
            int count = perFileCounts.getOrDefault(result.chunk().path(), 0);
            if (count >= MAX_RESULTS_PER_FILE) {
                continue;
            }
            limited.add(result);
            perFileCounts.put(result.chunk().path(), count + 1);
            if (limited.size() == topK) {
                break;
            }
        }
        return limited;
    }

    private RagSearchResult score(String query, double[] queryVector, Set<String> queryTokens, CodeChunk chunk) {
        double semantic = cosine(queryVector, chunk.vector());
        double token = tokenScore(queryTokens, chunk);
        double typeBonus = codeTypeBonus(query, chunk.codeType());
        double score = semantic * 0.70 + token * 0.25 + typeBonus;
        return new RagSearchResult(chunk, score, semantic, token, typeBonus);
    }

    private double tokenScore(Set<String> queryTokens, CodeChunk chunk) {
        if (queryTokens.isEmpty()) {
            return 0.0;
        }
        Set<String> contentTokens = new HashSet<>(tokenizer.tokenize(chunk.content()));
        Set<String> pathTokens = new HashSet<>(tokenizer.tokenize(chunk.path()));
        double score = 0.0;
        for (String token : queryTokens) {
            if (contentTokens.contains(token)) {
                score += 1.0;
            }
            if (pathTokens.contains(token)) {
                score += 0.5;
            }
        }
        return Math.min(1.0, score / queryTokens.size());
    }

    private double codeTypeBonus(String query, CodeType codeType) {
        String lower = query.toLowerCase(Locale.ROOT);
        if ((lower.contains("test") || lower.contains("测试")) && codeType == CodeType.TEST) {
            return 0.12;
        }
        if ((lower.contains("doc") || lower.contains("文档") || lower.contains("readme")) && codeType == CodeType.DOC) {
            return 0.12;
        }
        if ((lower.contains("config") || lower.contains("配置") || lower.contains("pom")) && codeType == CodeType.CONFIG) {
            return 0.12;
        }
        if ((lower.contains("code") || lower.contains("实现") || lower.contains("class")) && codeType == CodeType.SOURCE) {
            return 0.08;
        }
        return 0.0;
    }

    private double cosine(double[] left, double[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
