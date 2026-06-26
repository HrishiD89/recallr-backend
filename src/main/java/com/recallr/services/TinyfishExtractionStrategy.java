package com.recallr.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Component
public class TinyfishExtractionStrategy implements ExtractionStrategy {

    private final WebClient.Builder webClientBuilder;
    private final String apiKey;

    public TinyfishExtractionStrategy(WebClient.Builder webClientBuilder,
                                      @Value("${tinyfish.api.key:}") String apiKey) {
        this.webClientBuilder = webClientBuilder;
        this.apiKey = apiKey;
    }

    @Override
    public String extract(String url) throws Exception {
        // If the user hasn't configured an API key, we skip this fallback
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "";
        }

        WebClient client = webClientBuilder.clone()
                .baseUrl("https://api.fetch.tinyfish.ai")
                .build();

        TinyfishRequest requestBody = new TinyfishRequest(url);

        // Call the Tinyfish Fetch POST API
        TinyfishResponse response = client.post()
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TinyfishResponse.class)
                .block();

        // Safe extraction of the first result without using stream filters
        if (response != null && response.getResults() != null) {
            List<TinyfishResult> resultsList = response.getResults();
            if (!resultsList.isEmpty()) {
                TinyfishResult firstResult = resultsList.get(0);
                return firstResult.getText();
            }
        }

        return "";
    }

    @Override
    public String name() {
        return "TINYFISH";
    }

    // --- Simple Request / Response Helper classes ---
    
    public static class TinyfishRequest {
        private String[] urls;
        private String format = "markdown";

        public TinyfishRequest(String url) {
            this.urls = new String[]{url};
        }

        public String[] getUrls() {
            return urls;
        }

        public String getFormat() {
            return format;
        }
    }

    public static class TinyfishResponse {
        private List<TinyfishResult> results;

        public List<TinyfishResult> getResults() {
            return results;
        }

        public void setResults(List<TinyfishResult> results) {
            this.results = results;
        }
    }

    public static class TinyfishResult {
        private String url;
        private String text;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
