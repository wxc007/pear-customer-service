package com.pear.customerservice.conversation.dao;

import com.pear.customerservice.conversation.entity.ConversationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, Long> {

    List<ConversationMessageEntity> findByConversationIdOrderByIdAsc(Long conversationId);
}
