package com.pear.customerservice.conversation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "customer_conversation")
@Getter
@Setter
@NoArgsConstructor
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String guestTokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(columnDefinition = "TEXT")
    private String contextSummary;

    public ConversationEntity(String guestTokenHash, Instant expiresAt, Instant createdAt) {
        this.guestTokenHash = guestTokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }
}
