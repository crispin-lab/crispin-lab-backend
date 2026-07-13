CREATE TABLE space_visits (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    space_id BIGINT NOT NULL,
    last_visited_at TIMESTAMP NOT NULL
);
CREATE UNIQUE INDEX space_visits_user_id_space_id_uidx ON space_visits (user_id, space_id);
