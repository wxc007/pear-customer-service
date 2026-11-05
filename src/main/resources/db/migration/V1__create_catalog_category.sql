CREATE TABLE catalog_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_catalog_category_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
