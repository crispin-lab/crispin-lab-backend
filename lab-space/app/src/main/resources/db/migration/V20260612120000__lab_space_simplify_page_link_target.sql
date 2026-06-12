DELETE FROM page_links WHERE target_page_id IS NULL;
ALTER TABLE page_links DROP CONSTRAINT page_links_target_present_chk;
ALTER TABLE page_links DROP COLUMN target_url;
ALTER TABLE page_links ALTER COLUMN target_page_id SET NOT NULL;
