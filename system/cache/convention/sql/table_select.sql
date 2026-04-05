-- =============================================================================
-- table_select.sql
-- 테이블 정의 캐시 수집용 SQL (조회 전용)
-- DBMS: PostgreSQL
-- 근거: information_schema 표준 + 프로젝트 테이블 네이밍 (sy_, sm_, dt_, ps_, ss_ 등)
-- =============================================================================

-- 테이블 목록 조회
SELECT
    t.table_schema,
    t.table_name,
    obj_description((t.table_schema || '.' || t.table_name)::regclass, 'pg_class') AS table_comment
FROM information_schema.tables t
WHERE t.table_schema = 'public'
  AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name;

-- 테이블별 컬럼 목록 조회
SELECT
    c.table_schema,
    c.table_name,
    c.column_name,
    c.ordinal_position,
    c.data_type,
    c.character_maximum_length,
    c.numeric_precision,
    c.numeric_scale,
    c.is_nullable,
    c.column_default,
    col_description((c.table_schema || '.' || c.table_name)::regclass, c.ordinal_position) AS column_comment
FROM information_schema.columns c
WHERE c.table_schema = 'public'
ORDER BY c.table_name, c.ordinal_position;

-- PK 제약조건 조회
SELECT
    tc.table_name,
    tc.constraint_name,
    kcu.column_name,
    kcu.ordinal_position
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
WHERE tc.table_schema = 'public'
  AND tc.constraint_type = 'PRIMARY KEY'
ORDER BY tc.table_name, kcu.ordinal_position;

-- 인덱스 조회
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename, indexname;

-- TODO: table_schema가 'public'이 아닌 경우 스키마명 확인 필요
-- TODO: 파티션 테이블 존재 여부 확인 필요
