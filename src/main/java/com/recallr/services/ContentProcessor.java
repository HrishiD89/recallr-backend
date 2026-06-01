package com.recallr.services;

import com.recallr.client.GeminiClient;
import com.recallr.model.ExtractedDocument;
import com.recallr.model.ProcessingStatus;
import com.recallr.repository.ContentRepository;
import com.recallr.repository.ExtractedDocumentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
public class ContentProcessor {

    private final ContentRepository contentRepository;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final TextChunker textChunker;
    private final GeminiClient geminiClient;
    private final JdbcTemplate jdbcTemplate;

    public ContentProcessor(ContentRepository contentRepository,
                            ExtractedDocumentRepository extractedDocumentRepository,
                            TextChunker textChunker,
                            GeminiClient geminiClient,
                            JdbcTemplate jdbcTemplate) {
        this.contentRepository = contentRepository;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.textChunker = textChunker;
        this.geminiClient = geminiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ProcessingStatus process(Long contentId, Long userId){
        contentRepository.updateStatus(contentId, ProcessingStatus.PROCESSING);

        try{
            String rawText = extractedDocumentRepository
                    .findFirstByContentIdOrderByCreatedAtDesc(contentId)
                    .map(ExtractedDocument::getRawText)
                    .orElse("");

            if(rawText.isBlank()){
                contentRepository.updateStatus(contentId,ProcessingStatus.READY);
                return ProcessingStatus.READY;
            }

            List<String> chunks = textChunker.chunk(rawText);

            jdbcTemplate.update("DELETE FROM content_chunks WHERE content_id = ?", contentId);

            String sql = """
                INSERT INTO content_chunks (content_id, user_id, chunk_index, text, embedding)
                VALUES (?, ?, ?, ?, CAST(? AS vector))
            """;

            for(int i = 0 ; i < chunks.size() ; i++){
                String chunkText = chunks.get(i);
                float[] vector = geminiClient.embedText(chunkText);
                String vectorString = Arrays.toString(vector);

                jdbcTemplate.update(sql, contentId, userId, i, chunkText, vectorString);
            }

            contentRepository.updateStatus(contentId, ProcessingStatus.READY);
            return ProcessingStatus.READY;

        }catch (Exception e){
            jdbcTemplate.update("DELETE FROM content_chunks WHERE content_id = ?", contentId);
            contentRepository.updateStatus(contentId,ProcessingStatus.FAILED);
            return ProcessingStatus.FAILED;
        }
    }


}
