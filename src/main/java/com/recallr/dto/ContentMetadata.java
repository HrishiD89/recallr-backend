package com.recallr.dto;

import com.recallr.model.ContentType;

public record ContentMetadata(
        ContentType type,
        String embedUrl,
        String title,
        String thumbnail
) {}
