package com.pear.customerservice.knowledge.dto;

import com.pear.customerservice.knowledge.entity.KnowledgeRetrievalStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KnowledgeRetrievalResult {

    private final KnowledgeRetrievalStatus status;
    private final String context;
    private final String sourceAppendix;

    public static KnowledgeRetrievalResult empty(KnowledgeRetrievalStatus status) {
        return new KnowledgeRetrievalResult(status, "", "");
    }
}
