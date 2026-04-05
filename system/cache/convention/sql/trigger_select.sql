-- =============================================================================
-- trigger_select.sql
-- 트리거 정의 캐시 수집용 SQL (조회 전용)
-- DBMS: PostgreSQL
-- 근거: 프로젝트 소스에서 트리거 직접 참조는 확인되지 않았으나,
--        DB 레벨 제약/자동화가 존재할 수 있어 수집
-- =============================================================================

-- 트리거 목록 조회
SELECT
    trigger_schema,
    trigger_name,
    event_manipulation,
    event_object_schema,
    event_object_table,
    action_statement,
    action_timing,
    action_orientation
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- 트리거 함수 조회 (트리거가 호출하는 함수)
SELECT
    t.tgname AS trigger_name,
    c.relname AS table_name,
    p.proname AS function_name,
    pg_catalog.pg_get_functiondef(p.oid) AS function_definition,
    CASE t.tgtype & 2
        WHEN 2 THEN 'BEFORE'
        ELSE 'AFTER'
    END AS timing,
    CASE t.tgtype & 28
        WHEN 4 THEN 'INSERT'
        WHEN 8 THEN 'DELETE'
        WHEN 16 THEN 'UPDATE'
        WHEN 20 THEN 'INSERT OR UPDATE'
        WHEN 28 THEN 'INSERT OR UPDATE OR DELETE'
        ELSE 'OTHER'
    END AS event
FROM pg_catalog.pg_trigger t
JOIN pg_catalog.pg_class c ON c.oid = t.tgrelid
JOIN pg_catalog.pg_proc p ON p.oid = t.tgfoid
JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND NOT t.tgisinternal  -- 시스템 내부 트리거 제외
ORDER BY c.relname, t.tgname;

-- TODO: 트리거 사용 여부 확인 필요 (프로젝트에서 트리거 직접 참조 미확인)
-- TODO: 트리거 함수 정의 보안 정책 확인 필요
