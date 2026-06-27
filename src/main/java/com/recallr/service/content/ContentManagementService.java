package com.recallr.service.content;

import com.recallr.dto.ContentMetadata;
import com.recallr.dto.ExtractionResult;
import com.recallr.dto.request.ContentRequestDTO;
import com.recallr.dto.response.ContentResponseDTO;
import com.recallr.events.ContentCreatedEvent;
import com.recallr.model.Content;
import com.recallr.model.ProcessingStatus;
import com.recallr.model.User;
import com.recallr.repository.ContentRepository;
import com.recallr.repository.ExtractedDocumentRepository;
import com.recallr.service.extraction.TextExtractionService;
import com.recallr.service.embedding.ChunkEmbeddingService;
import com.recallr.service.quota.QuotaService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContentManagementService {

    private final ContentRepository contentRepository;
    private final ExtractedDocumentRepository extractedDocumentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ContentMetadataResolver resolver;
    private final TextExtractionService textExtractor;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final ApplicationEventPublisher eventPublisher;
    private final QuotaService quotaService;

    public ContentManagementService(ContentRepository contentRepository,
                                    ExtractedDocumentRepository extractedDocumentRepository,
                                    JdbcTemplate jdbcTemplate,
                                    ContentMetadataResolver resolver,
                                    TextExtractionService textExtractor,
                                    ChunkEmbeddingService chunkEmbeddingService,
                                    ApplicationEventPublisher eventPublisher,
                                    QuotaService quotaService) {
        this.contentRepository = contentRepository;
        this.extractedDocumentRepository = extractedDocumentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.resolver = resolver;
        this.textExtractor = textExtractor;
        this.chunkEmbeddingService = chunkEmbeddingService;
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

    @Transactional(readOnly = true)
    public ContentResponseDTO findById(Long id, User user) {
        Content content = contentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Content not found"));
        return toDTO(content);
    }

    @Transactional
    public void delete(Long id, User user) {
        Content content = contentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Content not found"));

        jdbcTemplate.update("DELETE FROM content_chunks WHERE content_id = ?", id);
        extractedDocumentRepository.findFirstByContentIdOrderByCreatedAtDesc(id)
                .ifPresent(extractedDocumentRepository::delete);
        contentRepository.delete(content);
    }

    @Transactional
    public ContentResponseDTO toggleRead(Long id, User user) {
        Content content = contentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Content not found"));
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