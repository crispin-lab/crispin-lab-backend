CREATE TABLE mentions (
    id BIGINT NOT NULL PRIMARY KEY,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    mentioned_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX mentions_source_user_uidx
    ON mentions (source_type, source_id, mentioned_user_id);
CREATE INDEX mentions_mentioned_user_id_idx ON mentions (mentioned_user_id);
