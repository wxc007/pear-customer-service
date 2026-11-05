package com.pear.customerservice.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "knowledge_chunk")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long versionId;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, unique = true, length = 36)
    private String vectorId;

    public KnowledgeChunkEntity(Long versionId, Integer chunkIndex, String content, String vectorId) {
        this.versionId = versionId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.vectorId = vectorId;
    }
}
