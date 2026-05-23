package com.recallr.dto;

import com.recallr.model.ContentType;

import java.time.LocalDateTime;

public record ContentResponseDTO(
        Long id,
        String url,
        String embedUrl,
        String title,
        String thumbnailUrl,
        ContentType type,
        boolean read,
        LocalDateTime createdAt
) {
}
