package com.pear.customerservice.knowledge.dao;

import com.pear.customerservice.knowledge.entity.KnowledgeIndexStatus;
import com.pear.customerservice.knowledge.entity.KnowledgePublicationStatus;
import com.pear.customerservice.knowledge.entity.KnowledgeVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KnowledgeVersionRepository extends JpaRepository<KnowledgeVersionEntity, Long> {

    Optional<KnowledgeVersionEntity> findByIdAndStatusAndIndexStatus(
            Long id, KnowledgePublicationStatus status, KnowledgeIndexStatus indexStatus
    );
}
