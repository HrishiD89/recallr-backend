package com.recallr.infrastructure.ai.gemini;

import java.util.List;

public record GeminiGenerateRequest(List<GeminiContent> contents) {
    public record GeminiContent(List<GeminiPart> parts) {}
    public record GeminiPart(String text) {}
}