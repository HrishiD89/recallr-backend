package com.recallr.domain.rag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
        @NotBlank
        String query,

        @Min(1)
        @Max(20)
        Integer topK
) {
    public int effectiveTopK() {
        return topK == null ? 8 : topK;
    }
}