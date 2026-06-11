ALTER TABLE page_links DROP COLUMN target;
ALTER TABLE page_links DROP COLUMN type;
ALTER TABLE page_links
    ADD CONSTRAINT page_links_target_present_chk
    CHECK ((target_page_id IS NOT NULL) <> (target_url IS NOT NULL));
