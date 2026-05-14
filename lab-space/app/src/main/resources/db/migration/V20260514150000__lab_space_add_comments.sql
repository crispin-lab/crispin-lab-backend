CREATE TABLE comments (
    id BIGINT NOT NULL PRIMARY KEY,
    page_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
CREATE INDEX comments_page_id_idx ON comments (page_id);
