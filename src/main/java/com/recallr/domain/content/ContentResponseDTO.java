package com.recallr.domain.content;

import java.time.LocalDateTime;

public record ContentResponseDTO(
        Long id,
        String url,
        String embedUrl,
        String title,
        String thumbnailUrl,
        ContentType type,
        ProcessingStatus processingStatus,
        boolean read,
        LocalDateTime createdAt,
        String author,
        String description,
        Integer wordCount
) {
}