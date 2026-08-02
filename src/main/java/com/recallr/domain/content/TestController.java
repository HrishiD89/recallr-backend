package com.recallr.domain.content;

import com.recallr.infrastructure.ai.gemini.GeminiClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    private final ContentMetadataResolver contentTypeResolver;
    private final TextExtractionService textExtractor;
    
    private final JsoupExtractionStrategy jsoupStrategy;
    private final JinaExtractionStrategy jinaStrategy;
    private final TinyfishExtractionStrategy tinyfishStrategy;

    public TestController(GeminiClient geminiClient,
                          ContentMetadataResolver contentTypeResolver,
                          TextExtractionService textExtractor,
                          JsoupExtractionStrategy jsoupStrategy,
                          JinaExtractionStrategy jinaStrategy,
                          TinyfishExtractionStrategy tinyfishStrategy) {
        this.geminiClient = geminiClient;
        this.contentTypeResolver = contentTypeResolver;
        this.textExtractor = textExtractor;
        this.jsoupStrategy = jsoupStrategy;
        this.jinaStrategy = jinaStrategy;
        this.tinyfishStrategy = tinyfishStrategy;
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
                "\nEmbedding: " + Arrays.toString(embedding);
    }

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
    public ExtractionResult testExtract(@RequestParam String url) {
        ContentType type = contentTypeResolver.resolve(url).type();
        return textExtractor.extract(url, type);
    }

    @GetMapping("/extract/jsoup")
    public String testJsoup(@RequestParam String url) throws Exception {
        return jsoupStrategy.extract(url);
    }

    @GetMapping("/extract/jina")
    public String testJina(@RequestParam String url) throws Exception {
        return jinaStrategy.extract(url);
    }

    @GetMapping("/extract/tinyfish")
    public String testTinyfish(@RequestParam String url) throws Exception {
        return tinyfishStrategy.extract(url);
    }
}