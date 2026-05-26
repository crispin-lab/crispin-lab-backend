CREATE TABLE space_members (
    id BIGINT NOT NULL PRIMARY KEY,
    space_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL
);
CREATE INDEX space_members_space_id_idx ON space_members (space_id);
CREATE INDEX space_members_user_id_idx ON space_members (user_id);
CREATE UNIQUE INDEX space_members_space_id_user_id_uidx ON space_members (space_id, user_id);
