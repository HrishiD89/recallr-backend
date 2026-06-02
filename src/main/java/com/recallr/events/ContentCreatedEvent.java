package com.recallr.events;

public record ContentCreatedEvent(Long contentId, String rawText, Long userId) {}
