package com.recallr.domain.content;

public record ContentMetadata(
        ContentType type,
        String embedUrl,
        String title,
        String thumbnail,
        String author,
        String description
) {}
