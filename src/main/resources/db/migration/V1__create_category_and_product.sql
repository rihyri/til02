CREATE TABLE categories (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    name      VARCHAR(255),
    parent_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories (id)
);

CREATE TABLE products (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(255),
    description VARCHAR(255),
    price       DECIMAL(19, 2),
    stock       INT,
    version     INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);