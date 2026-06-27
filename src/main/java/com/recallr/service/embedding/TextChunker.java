package com.recallr.service.embedding;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public static final int CHUNK_SIZE = 1400;
    public static final int OVERLAP = 200;

    public List<String> chunk(String rawText) {
        List<String> chunks = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return chunks;
        }
        int length = rawText.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            String chunk = rawText.substring(start, end);
            chunks.add(chunk);
            start += (CHUNK_SIZE - OVERLAP);
        }

        return chunks;

    }

}