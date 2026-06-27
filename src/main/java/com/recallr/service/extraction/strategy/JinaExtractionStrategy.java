package com.recallr.service.extraction.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JinaExtractionStrategy implements ExtractionStrategy {

    private final WebClient.Builder webClientBuilder;
    private final String apiKey;

    public JinaExtractionStrategy(WebClient.Builder webClientBuilder,
                                  @Value("${jina.api.key:}") String apiKey) {
        this.webClientBuilder = webClientBuilder;
        this.apiKey = apiKey;
    }

    @Override
    public String extract(String url) throws Exception {
        WebClient.Builder builder = webClientBuilder.clone()
                .baseUrl("https://r.jina.ai");

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        WebClient client = builder.build();

        // Perform HTTP GET request to r.jina.ai/<url>
        return client.get()
                .uri("/" + url)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Synchronous block since this runs in a background thread
    }

    @Override
    public String name() {
        return "JINA_READER";
    }
}