package com.recallr.dto;

public record ExtractionResult(String text, String extractedVia) {
    public boolean hasContent() {
        return text != null && text.trim().length() > 50; // Meaningful content threshold
    }
}
