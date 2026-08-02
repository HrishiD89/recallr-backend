package com.recallr.infrastructure.ai.gemini;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
public class GeminiClient implements GeminiApiClient {

    private final WebClient webClient;
    @Getter
    private final GeminiProperties props;

    public GeminiProperties getProps() { return props; }

    public GeminiClient(WebClient geminiWebClient, GeminiProperties props) {
        this.webClient = geminiWebClient;
        this.props = props;
    }

    @Override
    public float[] embedText(String text) {
        var request = new GeminiEmbedRequest(
                props.getEmbeddingModel(),
                new GeminiEmbedRequest.GeminiContent(
                        List.of(new GeminiEmbedRequest.GeminiPart(text))
                ),
                props.getEmbeddingDimensions()
        );

        GeminiEmbedResponse response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-embedding-001:embedContent")
                        .queryParam("key", props.getApiKey())
                        .build())
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Gemini embed error: " + body))
                )
                .bodyToMono(GeminiEmbedResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        return toFloatArray(response.embedding().values());
    }

    @Override
    public String generateAnswer(String prompt) {
        var request = new GeminiGenerateRequest(
                List.of(new GeminiGenerateRequest.GeminiContent(
                        List.of(new GeminiGenerateRequest.GeminiPart(prompt))
                ))
        );

        GeminiGenerateResponse response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-3.1-flash-lite" +
                                ":generateContent")
                        .queryParam("key", props.getApiKey())
                        .build())
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Gemini generate error: " + body))
                )
                .bodyToMono(GeminiGenerateResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        return response.candidates().get(0).content().parts().get(0).text();
    }

    private float[] toFloatArray(List<Double> values) {
        float[] arr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            arr[i] = values.get(i).floatValue();
        }
        return arr;
    }
}