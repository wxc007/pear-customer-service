CREATE TABLE customer_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    guest_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    context_summary TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_conversation_guest_token_hash UNIQUE (guest_token_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE conversation_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_message_conversation_id (conversation_id, id),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES customer_conversation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
