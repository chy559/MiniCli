package com.example.miniagent.memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class MemoryRetriever {

    public List<LongTermMemoryFact> retrieveRelevant(String query, List<LongTermMemoryFact> facts, int topK) {
        Set<String> queryTokens = tokenize(query);
        return facts.stream()
                .sorted(Comparator.comparingInt((LongTermMemoryFact fact) -> overlapCount(queryTokens, tokenize(fact.getContent()))).reversed()
                        .thenComparingLong(LongTermMemoryFact::getCreatedAt).reversed())
                .limit(topK)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int overlapCount(Set<String> left, Set<String> right) {
        int count = 0;
        for (String token : left) {
            if (right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream((text == null ? "" : text.toLowerCase(Locale.ROOT)).split("[^a-zA-Z0-9_\\u4e00-\\u9fa5]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}
