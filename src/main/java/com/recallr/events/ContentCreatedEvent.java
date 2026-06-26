package com.recallr.events;

public record ContentCreatedEvent(Long contentId, String rawText, String extractedVia, Long userId) {}