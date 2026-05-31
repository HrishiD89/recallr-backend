package com.recallr.repository;

import com.recallr.model.ExtractedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractedDocumentRepository extends JpaRepository<ExtractedDocument, Long> {
}
