CREATE INDEX pages_space_id_updated_at_id_idx
    ON pages (space_id, updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;
