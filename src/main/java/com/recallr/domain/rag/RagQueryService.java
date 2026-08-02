package com.recallr.domain.rag;

import com.recallr.infrastructure.ai.gemini.GeminiApiClient;

import com.recallr.domain.auth.User;
import com.recallr.domain.quota.QuotaService;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RagQueryService {

    private static final double MAX_RELEVANT_DISTANCE = 0.42;
    private static final Pattern CITATION_PATTERN = Pattern.compile("Source\\s+(\\d+)");

    private final GeminiApiClient geminiClient;
    private final RagSearchService ragSearchService;
    private final QuotaService quotaService;
    private final RagPromptBuilder promptBuilder;

    public RagQueryService(GeminiApiClient geminiClient,
                           RagSearchService ragSearchService,
                           QuotaService quotaService,
                           RagPromptBuilder promptBuilder) {
        this.geminiClient = geminiClient;
        this.ragSearchService = ragSearchService;
        this.quotaService = quotaService;
        this.promptBuilder = promptBuilder;
    }

    public RagQueryResponse query(RagQueryRequest request, User user) {
        quotaService.checkAndIncrementRagLimit(user);
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

        String prompt = promptBuilder.build(request.query(), relevant);
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
}