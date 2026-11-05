package com.pear.customerservice.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "knowledge_document_version")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 40)
    private String versionNumber;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String sourceContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgePublicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeIndexStatus indexStatus;

    @Column(length = 500)
    private String indexError;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant indexedAt;

    public KnowledgeVersionEntity(Long documentId, String versionNumber, String sourceContent,
                                  KnowledgePublicationStatus status, KnowledgeIndexStatus indexStatus, Instant createdAt) {
        this.documentId = documentId;
        this.versionNumber = versionNumber;
        this.sourceContent = sourceContent;
        this.status = status;
        this.indexStatus = indexStatus;
        this.createdAt = createdAt;
    }
}
