package com.pear.customerservice.conversation.entity;

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
@Table(name = "conversation_message")
@Getter
@Setter
@NoArgsConstructor
public class ConversationMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    public ConversationMessageEntity(Long conversationId, MessageRole role, String content, MessageStatus status,
                                     Instant createdAt) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }
}
