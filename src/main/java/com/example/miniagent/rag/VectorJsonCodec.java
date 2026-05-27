package com.example.miniagent.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VectorJsonCodec {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to encode vector", e);
        }
    }

    public double[] decode(String json) {
        try {
            return objectMapper.readValue(json, double[].class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to decode vector", e);
        }
    }
}
