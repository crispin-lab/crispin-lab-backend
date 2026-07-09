CREATE TABLE space_audit_entries (
    id BIGINT NOT NULL PRIMARY KEY,
    space_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    change_summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX space_audit_entries_space_id_created_at_id_idx
    ON space_audit_entries (space_id, created_at DESC, id DESC);
