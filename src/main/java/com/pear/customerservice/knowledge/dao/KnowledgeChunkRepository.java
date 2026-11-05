package com.pear.customerservice.knowledge.dao;

import com.pear.customerservice.knowledge.entity.KnowledgeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, Long> {

    List<KnowledgeChunkEntity> findByVersionIdOrderByChunkIndexAsc(Long versionId);
}
