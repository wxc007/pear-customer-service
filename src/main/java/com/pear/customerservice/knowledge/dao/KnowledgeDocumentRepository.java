package com.pear.customerservice.knowledge.dao;

import com.pear.customerservice.knowledge.entity.KnowledgeDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, Long> {
}
