package com.recallr.dto.response;

public record RagSourceDTO(
        Long contentId,
        String title,
        String url,
        Integer chunkIndex,
        Double distance
) {
}