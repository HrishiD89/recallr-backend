package com.recallr.event;

public record ContentCreatedEvent(Long contentId, String rawText, String extractedVia, Long userId) {}