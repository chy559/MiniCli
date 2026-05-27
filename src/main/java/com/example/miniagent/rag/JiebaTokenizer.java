package com.example.miniagent.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JiebaTokenizer {

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> tokens = new LinkedHashSet<>();
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String part : normalized.split("[^a-z0-9_\\u4e00-\\u9fa5]+")) {
            if (part.isBlank()) {
                continue;
            }
            addToken(tokens, part);
            addCamelAndSnakeParts(tokens, part);
            addChineseNgrams(tokens, part);
        }
        return new ArrayList<>(tokens);
    }

    private void addCamelAndSnakeParts(Set<String> tokens, String part) {
        for (String token : part.split("[_\\-]+")) {
            addToken(tokens, token);
        }
    }

    private void addChineseNgrams(Set<String> tokens, String part) {
        StringBuilder chinese = new StringBuilder();
        for (int i = 0; i < part.length(); i++) {
            char current = part.charAt(i);
            if (current >= '\u4e00' && current <= '\u9fa5') {
                chinese.append(current);
            }
        }
        if (chinese.length() == 0) {
            return;
        }
        for (int size = 1; size <= Math.min(3, chinese.length()); size++) {
            for (int i = 0; i <= chinese.length() - size; i++) {
                addToken(tokens, chinese.substring(i, i + size));
            }
        }
    }

    private void addToken(Set<String> tokens, String token) {
        if (token != null && token.length() >= 2) {
            tokens.add(token);
        }
    }
}
