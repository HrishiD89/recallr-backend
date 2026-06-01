package com.recallr.dto;

public record RagSourceDTO(
        Long contentId,
        String title,
        String url,
        Integer chunkIndex,
        Double distance
) {
}
