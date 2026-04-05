-- =============================================================================
-- view_select.sql
-- 뷰 정의 캐시 수집용 SQL (조회 전용)
-- DBMS: PostgreSQL
-- 근거: vw_dt_dstbt_shop 등 vw_ 접두사 뷰 확인 (dta030.xml 등)
-- =============================================================================

-- 뷰 목록 조회
SELECT
    v.table_schema,
    v.table_name AS view_name,
    v.view_definition,
    obj_description((v.table_schema || '.' || v.table_name)::regclass, 'pg_class') AS view_comment
FROM information_schema.views v
WHERE v.table_schema = 'public'
ORDER BY v.table_name;

-- 뷰 컬럼 목록 조회
SELECT
    c.table_schema,
    c.table_name AS view_name,
    c.column_name,
    c.ordinal_position,
    c.data_type,
    c.character_maximum_length,
    c.is_nullable
FROM information_schema.columns c
JOIN information_schema.views v
    ON c.table_schema = v.table_schema
    AND c.table_name = v.table_name
WHERE c.table_schema = 'public'
ORDER BY c.table_name, c.ordinal_position;

-- TODO: materialized view 존재 여부 확인 필요 (pg_matviews 카탈로그)
-- TODO: vw_ 접두사 외의 뷰 네이밍 패턴 확인 필요
