package com.example.miniagent.rag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class LocalHashEmbeddingModel {
    private static final int DIMENSIONS = 128;
    private final JiebaTokenizer tokenizer;

    public LocalHashEmbeddingModel(JiebaTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public double[] embed(String text) {
        double[] vector = new double[DIMENSIONS];
        List<String> tokens = tokenizer.tokenize(text);
        for (String token : tokens) {
            int hash = stableHash(token);
            int index = Math.floorMod(hash, DIMENSIONS);
            double sign = Math.floorMod(hash / DIMENSIONS, 2) == 0 ? 1.0 : -1.0;
            vector[index] += sign;
        }
        normalize(vector);
        return vector;
    }

    private int stableHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return ((bytes[0] & 0xff) << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff);
        } catch (NoSuchAlgorithmException e) {
            return token.hashCode();
        }
    }

    private void normalize(double[] vector) {
        double sum = 0.0;
        for (double value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
