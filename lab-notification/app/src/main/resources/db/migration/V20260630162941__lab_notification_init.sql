CREATE TABLE notifications (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX notifications_user_id_idx ON notifications (user_id);
CREATE UNIQUE INDEX notifications_user_type_source_uidx
    ON notifications (user_id, type, source_type, source_id);
