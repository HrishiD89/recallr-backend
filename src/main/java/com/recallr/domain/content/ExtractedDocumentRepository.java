package com.recallr.domain.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExtractedDocumentRepository extends JpaRepository<ExtractedDocument, Long> {
    Optional<ExtractedDocument> findFirstByContentIdOrderByCreatedAtDesc(Long contentId);
}
