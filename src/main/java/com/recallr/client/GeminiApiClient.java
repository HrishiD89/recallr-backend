package com.recallr.client;

public interface GeminiApiClient {

    float[] embedText(String text);

    String generateAnswer(String prompt);
}