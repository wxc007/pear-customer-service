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

@Entity
@Table(name = "knowledge_document")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private KnowledgeDocumentType documentType;

    @Column(nullable = false)
    private boolean demoData;

    public KnowledgeDocumentEntity(String title, KnowledgeDocumentType documentType, boolean demoData) {
        this.title = title;
        this.documentType = documentType;
        this.demoData = demoData;
    }
}
