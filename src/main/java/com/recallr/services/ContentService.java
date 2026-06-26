package com.recallr.services;

import com.recallr.dto.ContentMetadata;
import com.recallr.dto.ContentRequestDTO;
import com.recallr.dto.ContentResponseDTO;
import com.recallr.dto.ExtractionResult;
import com.recallr.events.ContentCreatedEvent;
import com.recallr.model.Content;
import com.recallr.model.ExtractedDocument;
import com.recallr.model.ProcessingStatus;
import com.recallr.model.User;
import com.recallr.repository.ContentRepository;
import com.recallr.repository.ExtractedDocumentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentTypeResolver resolver;
    private final TextExtractor textExtractor;
    private final ContentProcessingService contentProcessingService;
    private final ApplicationEventPublisher eventPublisher;
    private final QuotaService quotaService;

    public ContentService(ContentRepository contentRepository,
                          ContentTypeResolver resolver,
                          TextExtractor textExtractor,
                          ContentProcessor contentProcessor, ContentProcessingService contentProcessingService, ApplicationEventPublisher eventPublisher,QuotaService quotaService) {
        this.contentRepository = contentRepository;
        this.resolver = resolver;
        this.textExtractor = textExtractor;
        this.contentProcessingService = contentProcessingService;
        this.eventPublisher = eventPublisher;
        this.quotaService = quotaService;
    }

    @Transactional
    public ContentResponseDTO save(ContentRequestDTO request, User user) {
        quotaService.checkAndIncrementBookmarkLimit(user);
        ContentMetadata meta = resolver.resolve(request.url());
        Content content = new Content();
        content.setUrl(request.url());
        content.setEmbedUrl(meta.embedUrl());
        content.setThumbnailUrl(meta.thumbnail());
        content.setTitle(meta.title());
        content.setType(meta.type());
        content.setAuthor(meta.author());
        content.setDescription(meta.description());
        content.setUser(user);

        Content saved = contentRepository.save(content);

        ExtractionResult extraction = textExtractor.extract(request.url(), meta.type());

        // Pass extraction.text() and extraction.extractedVia() into the event
        eventPublisher.publishEvent(new ContentCreatedEvent(
                saved.getId(),
                extraction.text(),
                extraction.extractedVia(),
                user.getId()
        ));

        return toDTO(saved);

    }


    @Transactional(readOnly = true)
    public List<ContentResponseDTO> findAll(User user) {
        return contentRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public void delete(Long id, User user) {
        Content content = contentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        contentRepository.delete(content);
    }

    @Transactional
    public ContentResponseDTO toggleRead(Long id, User user) {
        Content content = contentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found"));
        content.setRead(!content.isRead());
        contentRepository.save(content);
        return toDTO(content);
    }

    private ContentResponseDTO toDTO(Content c) {
        return new ContentResponseDTO(
                c.getId(),
                c.getUrl(),
                c.getEmbedUrl(),
                c.getTitle(),
                c.getThumbnailUrl(),
                c.getType(),
                c.getProcessingStatus(),
                c.isRead(),
                c.getCreatedAt(),
                c.getAuthor(),
                c.getDescription(),
                c.getWordCount()
        );
    }

}
