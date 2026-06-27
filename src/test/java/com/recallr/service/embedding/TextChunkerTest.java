package com.recallr.service.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void chunk_shouldReturnEmptyListForNullInput() {
        List<String> chunks = chunker.chunk(null);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunk_shouldReturnEmptyListForBlankInput() {
        List<String> chunks = chunker.chunk("   ");
        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunk_shouldReturnSingleChunkForShortText() {
        String text = "Hello world";
        List<String> chunks = chunker.chunk(text);
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
    }

    @Test
    void chunk_shouldProduceMultipleChunksForLongText() {
        String text = "A".repeat(3000);
        List<String> chunks = chunker.chunk(text);
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void chunk_shouldMaintainOverlap() {
        String text = "A".repeat(2000);
        List<String> chunks = chunker.chunk(text);

        assertTrue(chunks.size() >= 2);
        // The overlap between consecutive chunks should be 200 chars
        for (int i = 0; i < chunks.size() - 1; i++) {
            String current = chunks.get(i);
            String next = chunks.get(i + 1);
            int expectedOverlapStart = current.length() - TextChunker.OVERLAP;
            if (expectedOverlapStart >= 0 && next.length() >= TextChunker.OVERLAP) {
                String overlapFromCurrent = current.substring(expectedOverlapStart);
                String overlapFromNext = next.substring(0, Math.min(TextChunker.OVERLAP, next.length()));
                assertEquals(overlapFromCurrent, overlapFromNext);
            }
        }
    }

    @Test
    void chunk_shouldNotExceedChunkSize() {
        String text = "B".repeat(5000);
        List<String> chunks = chunker.chunk(text);

        for (String chunk : chunks) {
            assertTrue(chunk.length() <= TextChunker.CHUNK_SIZE,
                    "Chunk length " + chunk.length() + " exceeds max " + TextChunker.CHUNK_SIZE);
        }
    }
}