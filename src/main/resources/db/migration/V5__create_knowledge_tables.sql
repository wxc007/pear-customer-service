CREATE TABLE knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    demo_data BOOLEAN NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE knowledge_document_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version_number VARCHAR(40) NOT NULL,
    source_content MEDIUMTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    index_status VARCHAR(20) NOT NULL,
    index_error VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    indexed_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_document_version UNIQUE (document_id, version_number),
    CONSTRAINT fk_knowledge_version_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE knowledge_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    vector_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_chunk_position UNIQUE (version_id, chunk_index),
    CONSTRAINT uk_knowledge_chunk_vector UNIQUE (vector_id),
    CONSTRAINT fk_knowledge_chunk_version FOREIGN KEY (version_id) REFERENCES knowledge_document_version (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
