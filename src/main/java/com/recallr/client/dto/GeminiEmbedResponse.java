package com.recallr.client.dto;

import java.util.List;

public record GeminiEmbedResponse(GeminiEmbedding embedding) {
    public record GeminiEmbedding(List<Double> values) {}
}