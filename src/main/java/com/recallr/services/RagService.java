package com.recallr.services;

import com.recallr.client.GeminiClient;
import com.recallr.dto.RagQueryRequest;
import com.recallr.dto.RagQueryResponse;
import com.recallr.dto.RagSearchResult;
import com.recallr.dto.RagSourceDTO;
import com.recallr.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RagService {

    private static final double MAX_RELEVANT_DISTANCE = 0.42;
    private static final Pattern CITATION_PATTERN = Pattern.compile("Source\\s+(\\d+)");

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


        List<RagSearchResult> relevant = matches.stream()
                .filter(m -> m.distance() <= MAX_RELEVANT_DISTANCE)
                .toList();

        if (relevant.isEmpty()) {
            return new RagQueryResponse(
                    "I could not find relevant saved content to answer this.",
                    List.of()
            );
        }

        String prompt = buildPrompt(request.query(), relevant);
        String answer = geminiClient.generateAnswer(prompt);

        List<RagSourceDTO> sources = citedSources(answer, relevant).stream()
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

    private List<RagSearchResult> citedSources(String answer, List<RagSearchResult> relevant) {
        Set<Integer> cited = new HashSet<>();
        Matcher m = CITATION_PATTERN.matcher(answer);
        while (m.find()) cited.add(Integer.parseInt(m.group(1)));

        List<RagSearchResult> citedMatches = IntStream.range(0, relevant.size())
                .filter(i -> cited.contains(i + 1))
                .mapToObj(relevant::get)
                .toList();

        // Collapse multiple cited chunks from the same document into one source —
        // keep whichever chunk had the lowest (best) distance as the representative.
        return citedMatches.stream()
                .collect(Collectors.toMap(
                        RagSearchResult::contentId,
                        r -> r,
                        (a, b) -> a.distance() <= b.distance() ? a : b,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .sorted(Comparator.comparingDouble(RagSearchResult::distance))
                .toList();
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

                Rules:
                - Answer in at most 3 sentences. No preamble, no restating the question.
                - If the saved content doesn't contain enough information, say in one sentence that you couldn't find it. Do not guess.
                - Cite inline using [Source 1], [Source 2], etc. — one source number per bracket. If a sentence draws on multiple sources, use separate brackets like [Source 1][Source 3], never [Source 1, Source 3].
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
