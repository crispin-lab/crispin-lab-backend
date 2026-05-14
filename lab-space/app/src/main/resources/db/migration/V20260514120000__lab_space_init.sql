CREATE TABLE spaces (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE pages (
    id BIGINT NOT NULL PRIMARY KEY,
    space_id BIGINT NOT NULL,
    parent_page_id BIGINT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    current_version INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX pages_space_id_idx ON pages (space_id);
CREATE INDEX pages_parent_page_id_idx ON pages (parent_page_id);

CREATE TABLE page_revisions (
    id BIGINT NOT NULL PRIMARY KEY,
    page_id BIGINT NOT NULL,
    version INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX page_revisions_page_id_version_uidx ON page_revisions (page_id, version);

CREATE TABLE page_links (
    id BIGINT NOT NULL PRIMARY KEY,
    page_id BIGINT NOT NULL,
    revision_id BIGINT NOT NULL,
    target VARCHAR(500) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX page_links_page_id_idx ON page_links (page_id);
CREATE INDEX page_links_revision_id_idx ON page_links (revision_id);
