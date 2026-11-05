package com.pear.customerservice.conversation.dao;

import com.pear.customerservice.conversation.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {
}
