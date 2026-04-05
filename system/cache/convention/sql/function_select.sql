-- =============================================================================
-- function_select.sql
-- 함수 정의 캐시 수집용 SQL (조회 전용)
-- DBMS: PostgreSQL
-- 근거: fn_ps_get_brndz_nm, fn_sy_get_code_nm, fn_ss_get_shop_nm 등
--        sp_dstbt_prdt_auto_crt 등 (dta030.xml, dtb010.xml 등에서 확인)
-- =============================================================================

-- 사용자 정의 함수 목록 조회
SELECT
    n.nspname AS schema_name,
    p.proname AS function_name,
    pg_catalog.pg_get_function_arguments(p.oid) AS arguments,
    pg_catalog.pg_get_function_result(p.oid) AS return_type,
    CASE p.prokind
        WHEN 'f' THEN 'function'
        WHEN 'p' THEN 'procedure'
        WHEN 'a' THEN 'aggregate'
        WHEN 'w' THEN 'window'
    END AS function_type,
    d.description AS function_comment,
    pg_catalog.pg_get_functiondef(p.oid) AS function_definition
FROM pg_catalog.pg_proc p
JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
LEFT JOIN pg_catalog.pg_description d ON d.objoid = p.oid
WHERE n.nspname = 'public'
  AND p.prokind IN ('f', 'p')  -- function과 procedure만
ORDER BY p.proname;

-- fn_ 접두사 함수만 조회 (코드명 변환 함수)
SELECT
    p.proname AS function_name,
    pg_catalog.pg_get_function_arguments(p.oid) AS arguments,
    pg_catalog.pg_get_function_result(p.oid) AS return_type
FROM pg_catalog.pg_proc p
JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = 'public'
  AND p.proname LIKE 'fn_%'
ORDER BY p.proname;

-- sp_ 접두사 프로시저만 조회
SELECT
    p.proname AS procedure_name,
    pg_catalog.pg_get_function_arguments(p.oid) AS arguments
FROM pg_catalog.pg_proc p
JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = 'public'
  AND p.proname LIKE 'sp_%'
  AND p.prokind = 'p'
ORDER BY p.proname;

-- TODO: schema가 'public'이 아닌 경우 확인 필요
-- TODO: pg_catalog.pg_get_functiondef()가 보안 정책으로 차단될 수 있음
