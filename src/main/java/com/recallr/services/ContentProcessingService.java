package com.recallr.services;

import com.recallr.events.ContentCreatedEvent;
import com.recallr.model.Content;
import com.recallr.model.ExtractedDocument;
import com.recallr.model.ProcessingStatus;
import com.recallr.repository.ContentRepository;
import com.recallr.repository.ExtractedDocumentRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class ContentProcessingService {

    private final ContentProcessor contentProcessor;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final ContentRepository contentRepository;

    public ContentProcessingService(ContentProcessor contentProcessor, ExtractedDocumentRepository extractedDocumentRepository,ContentRepository contentRepository) {
        this.contentProcessor = contentProcessor;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.contentRepository = contentRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processContent(ContentCreatedEvent event) {

        Content content = contentRepository.findById(event.contentId())
                .orElseThrow(() -> new RuntimeException("Content not found: " + event.contentId()));

        ExtractedDocument doc = new ExtractedDocument();
        doc.setContent(content);
        doc.setSourceKind(content.getType());
        doc.setRawText(event.rawText());
        doc.setExtractedVia(event.extractedVia());
        extractedDocumentRepository.save(doc);

        ProcessingStatus finalStatus = contentProcessor.process(content.getId(), event.userId());
        content.setProcessingStatus(finalStatus);
        contentRepository.save(content);

    }
}
