package com.recallr.infrastructure.ai.gemini;

public interface GeminiApiClient {

    float[] embedText(String text);

    String generateAnswer(String prompt);
}