package com.recallr.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "extracted_documents")
public class ExtractedDocument {

    @Id
    @GeneratedValue(generator = "tsid")
    @GenericGenerator(name = "tsid", type = com.recallr.config.TsidGenerator.class)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 32)
    private ContentType sourceKind;

    @Setter
    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Setter
    @Column(name = "extracted_via", length = 32)
    private String extractedVia;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ExtractedDocument() {}

}