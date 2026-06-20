package com.recallr.controller;

import com.recallr.client.GeminiClient;
import com.recallr.model.ContentType;
import com.recallr.services.ContentTypeResolver;
import com.recallr.services.TextExtractor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private final GeminiClient geminiClient;
    private final ContentTypeResolver contentTypeResolver;
    private final TextExtractor textExtractor;

    public TestController(GeminiClient geminiClient,
                          ContentTypeResolver contentTypeResolver,
                          TextExtractor textExtractor) {
        this.geminiClient = geminiClient;
        this.contentTypeResolver = contentTypeResolver;
        this.textExtractor = textExtractor;
    }

    @GetMapping("/check")
    public String checkConfig() {
        return "API key starts with: " +
                geminiClient.getProps().getApiKey().substring(0, 10);
    }

    @GetMapping("/embed")
    public String testEmbed(@RequestParam String text) {
        float[] embedding = geminiClient.embedText(text);
        return "Embedding length: " + embedding.length +
                "\nEmbedding: " + Arrays.toString(embedding);    }

    @GetMapping("/generate")
    public String testGenerate() {
        String answer = geminiClient.generateAnswer(
                "What is pgvector and why is it used in RAG applications? Answer in 2 sentences."
        );
        return answer;
    }

    @GetMapping("/me")
    public Map<String, Object> me(
            @AuthenticationPrincipal Jwt jwt) {

        return jwt.getClaims();
    }

    @GetMapping("/extract")
    public String testExtract(@RequestParam String url) {
        ContentType type = contentTypeResolver.resolve(url).type();
        return textExtractor.extract(url, type);
    }
}