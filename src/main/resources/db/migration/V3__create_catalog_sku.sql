CREATE TABLE catalog_sku (
    id BIGINT NOT NULL AUTO_INCREMENT,
    spu_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    price DECIMAL(18, 2) NOT NULL,
    sale_status VARCHAR(20) NOT NULL,
    attributes JSON NOT NULL,
    demo_data BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT fk_catalog_sku_spu FOREIGN KEY (spu_id) REFERENCES catalog_spu (id),
    CONSTRAINT ck_catalog_sku_price_nonnegative CHECK (price >= 0),
    INDEX idx_catalog_sku_spu (spu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
