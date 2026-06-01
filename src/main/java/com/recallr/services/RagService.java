package com.recallr.services;

import com.recallr.client.GeminiClient;
import com.recallr.dto.RagQueryRequest;
import com.recallr.dto.RagQueryResponse;
import com.recallr.dto.RagSearchResult;
import com.recallr.dto.RagSourceDTO;
import com.recallr.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private final GeminiClient geminiClient;
    private final RagSearchService ragSearchService;

    public RagService(GeminiClient geminiClient, RagSearchService ragSearchService) {
        this.geminiClient = geminiClient;
        this.ragSearchService = ragSearchService;
    }

    public RagQueryResponse query(RagQueryRequest request, User user) {
        float[] queryEmbedding = geminiClient.embedText(request.query());
        List<RagSearchResult> matches = ragSearchService.search(
                user.getId(),
                queryEmbedding,
                request.effectiveTopK()
        );

        if (matches.isEmpty()) {
            return new RagQueryResponse(
                    "I could not find relevant saved content to answer this.",
                    List.of()
            );
        }

        String prompt = buildPrompt(request.query(), matches);
        String answer = geminiClient.generateAnswer(prompt);

        List<RagSourceDTO> sources = matches.stream()
                .map(match -> new RagSourceDTO(
                        match.contentId(),
                        match.title(),
                        match.url(),
                        match.chunkIndex(),
                        match.distance()
                ))
                .toList();

        return new RagQueryResponse(answer, sources);
    }

    private String buildPrompt(String query, List<RagSearchResult> matches) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < matches.size(); i++) {
            RagSearchResult match = matches.get(i);
            context.append("Source ")
                    .append(i + 1)
                    .append(" | title: ")
                    .append(nullToUnknown(match.title()))
                    .append(" | url: ")
                    .append(match.url())
                    .append("\n")
                    .append(match.text())
                    .append("\n\n");
        }

        return """
                You are Recallr, an assistant that answers using only the user's saved content.
                If the context does not contain enough information, say that you could not find it in the saved content.
                Cite sources inline using [Source 1], [Source 2], etc.

                User question:
                %s

                Saved content context:
                %s

                Answer:
                """.formatted(query, context);
    }

    private String nullToUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown title" : value;
    }
}
