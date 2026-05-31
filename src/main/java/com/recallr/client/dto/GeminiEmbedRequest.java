package com.recallr.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GeminiEmbedRequest(
        String model,
        GeminiContent content,
        @JsonProperty("outputDimensionality") int outputDimensionality
) {
    public record GeminiContent(List<GeminiPart> parts) {}
    public record GeminiPart(String text) {}
}