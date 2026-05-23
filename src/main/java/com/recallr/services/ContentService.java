package com.recallr.services;

import com.recallr.dto.ContentMetadata;
import com.recallr.dto.ContentRequestDTO;
import com.recallr.dto.ContentResponseDTO;
import com.recallr.model.Content;
import com.recallr.model.User;
import com.recallr.repository.ContentRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContentService {

    private final ContentRepository contentRepository;
    private final ContentTypeResolver resolver;

    public ContentService(ContentRepository contentRepository, ContentTypeResolver resolver) {
        this.contentRepository = contentRepository;
        this.resolver = resolver;
    }

    @Transactional
    public ContentResponseDTO save(ContentRequestDTO request, User user) {
        ContentMetadata meta = resolver.resolve(request.url());
        Content content = new Content();
        content.setUrl(request.url());
        content.setEmbedUrl(meta.embedUrl());
        content.setThumbnailUrl(meta.thumbnail());
        content.setTitle(meta.title());
        content.setType(meta.type());
        content.setUser(user);

        Content saved = contentRepository.save(content);

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
                c.isRead(),
                c.getCreatedAt()
        );
    }

}
