-- 기존 page / comment 본문의 @mention 노드를 mentions 테이블에 seed.
-- 목적: LAB-147 첫 edit 시 prior mention set 가 비어 있어 모든 user 가 newlyAdded 로
--      판정돼 NotificationInbox 폭발하는 회귀 방지.
-- backfill 한 mention 은 dispatcher 의 prior set 비교 기준이 된다.
-- IDs: ROW_NUMBER 1.. 사용. Snowflake epoch (2025-01-01) 후 ID 는 2^57 이상이라 충돌 없음.
-- 본문이 JSON 아닌 row 는 silently skip (PL/pgSQL EXCEPTION 처리).

DO $backfill$
DECLARE
    page_record RECORD;
    comment_record RECORD;
    mention_node JSONB;
    user_id_text TEXT;
    user_id_value BIGINT;
    next_id BIGINT := 1;
BEGIN
    FOR page_record IN
        SELECT id, author_id, content, updated_at FROM pages WHERE deleted_at IS NULL
    LOOP
        BEGIN
            FOR mention_node IN
                SELECT node
                FROM jsonb_path_query(page_record.content::jsonb, '$.**'::jsonpath) AS node
                WHERE node->>'type' = 'mention'
            LOOP
                user_id_text := mention_node->'attrs'->>'userId';
                CONTINUE WHEN user_id_text IS NULL OR user_id_text !~ '^[0-9]+$';
                user_id_value := user_id_text::bigint;
                CONTINUE WHEN user_id_value = page_record.author_id;
                INSERT INTO mentions (
                    id, source_type, source_id, mentioned_user_id, mentioned_by_user_id, created_at
                ) VALUES (
                    next_id, 'PAGE', page_record.id, user_id_value,
                    page_record.author_id, page_record.updated_at
                )
                ON CONFLICT (source_type, source_id, mentioned_user_id) DO NOTHING;
                next_id := next_id + 1;
            END LOOP;
        EXCEPTION WHEN invalid_text_representation OR datatype_mismatch OR numeric_value_out_of_range THEN
            CONTINUE;
        END;
    END LOOP;

    FOR comment_record IN
        SELECT id, author_id, content, updated_at FROM comments WHERE deleted_at IS NULL
    LOOP
        BEGIN
            FOR mention_node IN
                SELECT node
                FROM jsonb_path_query(comment_record.content::jsonb, '$.**'::jsonpath) AS node
                WHERE node->>'type' = 'mention'
            LOOP
                user_id_text := mention_node->'attrs'->>'userId';
                CONTINUE WHEN user_id_text IS NULL OR user_id_text !~ '^[0-9]+$';
                user_id_value := user_id_text::bigint;
                CONTINUE WHEN user_id_value = comment_record.author_id;
                INSERT INTO mentions (
                    id, source_type, source_id, mentioned_user_id, mentioned_by_user_id, created_at
                ) VALUES (
                    next_id, 'COMMENT', comment_record.id, user_id_value,
                    comment_record.author_id, comment_record.updated_at
                )
                ON CONFLICT (source_type, source_id, mentioned_user_id) DO NOTHING;
                next_id := next_id + 1;
            END LOOP;
        EXCEPTION WHEN invalid_text_representation OR datatype_mismatch OR numeric_value_out_of_range THEN
            CONTINUE;
        END;
    END LOOP;
END $backfill$;
