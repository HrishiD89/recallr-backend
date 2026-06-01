package com.recallr.dto;

public record RagSearchResult(
        Long contentId,
        String title,
        String url,
        Integer chunkIndex,
        String text,
        Double distance
) {
}
