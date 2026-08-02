package com.recallr.domain.rag;

import com.recallr.domain.content.Content;
import com.recallr.domain.content.ContentProcessingService;
import com.recallr.event.ContentCreatedEvent;

import com.recallr.domain.content.ExtractedDocument;
import com.recallr.domain.content.ProcessingStatus;
import com.recallr.domain.content.ContentRepository;
import com.recallr.domain.content.ExtractedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ChunkEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkEmbeddingService.class);

    private final ContentRepository contentRepository;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final ContentProcessingService contentProcessingService;

    public ChunkEmbeddingService(ContentRepository contentRepository,
                                 ExtractedDocumentRepository extractedDocumentRepository,
                                 ContentProcessingService contentProcessingService) {
        this.contentRepository = contentRepository;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.contentProcessingService = contentProcessingService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processContent(com.recallr.event.ContentCreatedEvent event) {
        com.recallr.domain.content.Content content;
        try {
            content = contentRepository.findById(event.contentId())
                    .orElseThrow(() -> new RuntimeException("Content not found: " + event.contentId()));

            ExtractedDocument doc = new ExtractedDocument();
            doc.setContent(content);
            doc.setSourceKind(content.getType());
            doc.setRawText(event.rawText());
            doc.setExtractedVia(event.extractedVia());
            extractedDocumentRepository.save(doc);

            ProcessingStatus finalStatus = contentProcessingService.process(content.getId(), event.userId());
            content.setProcessingStatus(finalStatus);

            if (event.rawText() != null && !event.rawText().trim().isEmpty()) {
                String[] words = event.rawText().trim().split("\\s+");
                content.setWordCount(words.length);
            } else {
                content.setWordCount(0);
            }

            contentRepository.save(content);
        } catch (Exception e) {
            log.error("Process content failed for contentId={}", event.contentId(), e);
            contentRepository.findById(event.contentId()).ifPresent(c -> {
                c.setProcessingStatus(ProcessingStatus.FAILED);
                contentRepository.save(c);
            });
        }
    }
}