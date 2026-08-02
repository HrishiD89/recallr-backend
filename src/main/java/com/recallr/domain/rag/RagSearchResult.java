package com.recallr.domain.rag;

public record RagSearchResult(
        Long contentId,
        String title,
        String url,
        Integer chunkIndex,
        String text,
        Double distance
) {
}
