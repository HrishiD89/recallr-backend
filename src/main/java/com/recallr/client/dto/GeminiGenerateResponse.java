package com.recallr.client.dto;

import java.util.List;

public record GeminiGenerateResponse(List<GeminiCandidate> candidates) {
    public record GeminiCandidate(GeminiContent content) {}
    public record GeminiContent(List<GeminiPart> parts) {}
    public record GeminiPart(String text) {}
}