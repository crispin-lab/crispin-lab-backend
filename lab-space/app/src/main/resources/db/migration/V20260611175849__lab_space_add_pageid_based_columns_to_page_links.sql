ALTER TABLE page_links ADD COLUMN target_page_id BIGINT NULL;
ALTER TABLE page_links ADD COLUMN target_url VARCHAR(500) NULL;
CREATE INDEX page_links_target_page_id_idx
    ON page_links (target_page_id)
    WHERE target_page_id IS NOT NULL;
