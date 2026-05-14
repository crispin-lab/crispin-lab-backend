CREATE TABLE tags (
    id BIGINT NOT NULL PRIMARY KEY,
    space_id BIGINT NOT NULL,
    name VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX tags_space_id_idx ON tags (space_id);
CREATE UNIQUE INDEX tags_space_id_name_uidx ON tags (space_id, name);

CREATE TABLE page_tags (
    page_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (page_id, tag_id)
);
CREATE INDEX page_tags_tag_id_idx ON page_tags (tag_id);
