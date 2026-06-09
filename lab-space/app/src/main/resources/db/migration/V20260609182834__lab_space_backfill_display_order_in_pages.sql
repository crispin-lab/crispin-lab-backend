WITH ordered AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY space_id, parent_page_id
               ORDER BY created_at, id
           ) - 1 AS rn
    FROM pages
    WHERE deleted_at IS NULL
)
UPDATE pages
SET display_order = ordered.rn
FROM ordered
WHERE pages.id = ordered.id;
