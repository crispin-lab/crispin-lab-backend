-- pages.content / page_links 의 legacy `[[title]]` 데이터를 pageId 기반 syntax 로 backfill.
-- INTERNAL 식별자 ambiguity (같은 space 내 동명 페이지 0건 또는 2건 이상) 인 경우 마스킹 텍스트로 치환.
-- self-link (`[[자기 페이지 제목]]`) 는 의도상 다른 페이지를 가리키는 일반적 case 라 lookup 에서 제외.
-- MASKED 리터럴 '비공개 페이지' 는 PageLinkMaskingPolicy.MASKED_DISPLAY_TEXT 와 동기화 유지.

DO $$
DECLARE
    pattern CONSTANT TEXT := '\[\[([^|\[\]]+)(?:\|([^\[\]|]+))?\]\]';
    page_row RECORD;
    parts TEXT[];
    matches TEXT[][];
    target_text TEXT;
    alias_text TEXT;
    match_count INTEGER;
    resolved_id BIGINT;
    replacement TEXT;
    new_content TEXT;
    n INTEGER;
    i INTEGER;
BEGIN
    FOR page_row IN SELECT id, space_id, content FROM pages LOOP
        -- regexp_split_to_array 는 본문을 매치 위치 기준으로 잘라 [parts[1], match, parts[2], match, ..., parts[n+1]] 형태로 분해.
        -- regexp_matches 의 row 결과를 2D 배열로 모아 같은 순서로 정렬, 위치 기반 재조립으로 동일 substring 의 중복 치환을 방지.
        parts := regexp_split_to_array(page_row.content, pattern);
        matches :=
            ARRAY(
                SELECT regexp_matches(page_row.content, pattern, 'g')
            );
        n := COALESCE(array_length(matches, 1), 0);
        IF n = 0 THEN
            CONTINUE;
        END IF;
        new_content := parts[1];
        FOR i IN 1..n LOOP
            target_text := matches[i][1];
            alias_text := matches[i][2];
            IF target_text ~* '^https?://' THEN
                -- EXTERNAL 은 새 syntax 가 그대로 인식하므로 원본 유지.
                replacement := '[[' || target_text ||
                               COALESCE('|' || alias_text, '') || ']]';
            ELSE
                SELECT COUNT(*), MIN(id) INTO match_count, resolved_id
                FROM pages
                WHERE space_id = page_row.space_id
                  AND title = target_text
                  AND deleted_at IS NULL
                  AND id <> page_row.id;
                IF match_count = 1 THEN
                    replacement := '[[pageId:' || resolved_id || '|' ||
                                   COALESCE(alias_text, target_text) || ']]';
                ELSE
                    replacement := '비공개 페이지';
                    RAISE WARNING
                        'page_id=% wiki-link backfill ambiguous title=% count=%',
                        page_row.id, target_text, match_count;
                END IF;
            END IF;
            new_content := new_content || replacement || parts[i + 1];
        END LOOP;
        IF new_content <> page_row.content THEN
            UPDATE pages SET content = new_content WHERE id = page_row.id;
        END IF;
    END LOOP;
END $$;

DO $$
DECLARE
    link_row RECORD;
    match_count INTEGER;
    resolved_id BIGINT;
BEGIN
    FOR link_row IN
        SELECT pl.id AS link_id,
               pl.page_id AS src_page_id,
               pl.target,
               pl.type,
               p.space_id AS src_space_id
        FROM page_links pl
        JOIN pages p ON p.id = pl.page_id
    LOOP
        -- target 패턴 우선 분기 (http(s) prefix 우선) — type 컬럼은 보조 검증 용도.
        -- INTERNAL 만 title lookup, EXTERNAL 인데 target 이 http(s) 가 아니면 데이터 오염 방지 차원에서 DELETE + WARNING.
        IF link_row.target ~* '^https?://' THEN
            UPDATE page_links
            SET target_url = link_row.target
            WHERE id = link_row.link_id;
        ELSIF link_row.type = 'INTERNAL' THEN
            SELECT COUNT(*), MIN(id) INTO match_count, resolved_id
            FROM pages
            WHERE space_id = link_row.src_space_id
              AND title = link_row.target
              AND deleted_at IS NULL
              AND id <> link_row.src_page_id;
            IF match_count = 1 THEN
                UPDATE page_links
                SET target_page_id = resolved_id
                WHERE id = link_row.link_id;
            ELSE
                DELETE FROM page_links WHERE id = link_row.link_id;
                RAISE WARNING
                    'page_links id=% backfill ambiguous title=% count=%',
                    link_row.link_id, link_row.target, match_count;
            END IF;
        ELSIF link_row.type = 'EXTERNAL' THEN
            DELETE FROM page_links WHERE id = link_row.link_id;
            RAISE WARNING
                'page_links id=% has non-http external target=%',
                link_row.link_id, link_row.target;
        ELSE
            RAISE EXCEPTION
                'page_links id=% has unknown type=%',
                link_row.link_id, link_row.type;
        END IF;
    END LOOP;
END $$;
