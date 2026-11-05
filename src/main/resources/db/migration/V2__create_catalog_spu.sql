CREATE TABLE catalog_spu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    product_type VARCHAR(30) NOT NULL,
    sale_status VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    attributes JSON NOT NULL,
    demo_data BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_catalog_spu_category FOREIGN KEY (category_id) REFERENCES catalog_category (id),
    INDEX idx_catalog_spu_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
