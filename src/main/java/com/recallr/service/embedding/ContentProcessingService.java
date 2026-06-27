package com.recallr.service.embedding;

import com.recallr.client.GeminiApiClient;
import com.recallr.model.ExtractedDocument;
import com.recallr.model.ProcessingStatus;
import com.recallr.repository.ContentRepository;
import com.recallr.repository.ExtractedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class ContentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ContentProcessingService.class);

    private final ContentRepository contentRepository;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final TextChunker textChunker;
    private final GeminiApiClient geminiClient;
    private final JdbcTemplate jdbcTemplate;

    public ContentProcessingService(ContentRepository contentRepository,
                                    ExtractedDocumentRepository extractedDocumentRepository,
                                    TextChunker textChunker,
                                    GeminiApiClient geminiClient,
                                    JdbcTemplate jdbcTemplate) {
        this.contentRepository = contentRepository;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.textChunker = textChunker;
        this.geminiClient = geminiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ProcessingStatus process(Long contentId, Long userId) {
        contentRepository.updateStatus(contentId, ProcessingStatus.PROCESSING);

        try {
            String rawText = extractedDocumentRepository
                    .findFirstByContentIdOrderByCreatedAtDesc(contentId)
                    .map(ExtractedDocument::getRawText)
                    .orElse("");

            if (rawText.isBlank()) {
                contentRepository.updateStatus(contentId, ProcessingStatus.READY);
                return ProcessingStatus.READY;
            }

            List<String> chunks = textChunker.chunk(rawText);

            jdbcTemplate.update("DELETE FROM content_chunks WHERE content_id = ?", contentId);

            String sql = """
                INSERT INTO content_chunks (content_id, user_id, chunk_index, text, embedding)
                VALUES (?, ?, ?, ?, CAST(? AS vector))
            """;

            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] vector = geminiClient.embedText(chunkText);
                String vectorString = Arrays.toString(vector);

                jdbcTemplate.update(sql, contentId, userId, i, chunkText, vectorString);
            }

            contentRepository.updateStatus(contentId, ProcessingStatus.READY);
            return ProcessingStatus.READY;

        } catch (Exception e) {
            log.error("Chunk embedding failed for contentId={}", contentId, e);
            jdbcTemplate.update("DELETE FROM content_chunks WHERE content_id = ?", contentId);
            contentRepository.updateStatus(contentId, ProcessingStatus.FAILED);
            return ProcessingStatus.FAILED;
        }
    }
}