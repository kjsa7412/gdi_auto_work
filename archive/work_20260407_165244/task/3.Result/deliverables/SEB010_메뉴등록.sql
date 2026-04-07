-- =============================================================================
-- SEB010/SEB011/SEB012 프로그램 및 메뉴 등록 SQL
-- 대상 테이블: sy_pgm_info, sy_menu_info
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. sy_pgm_info 등록 (SEB010 - 사전검토관리, 메인화면)
-- ---------------------------------------------------------------------------
INSERT INTO sy_pgm_info (
    pgm_id, pgm_nm, pgm_path,
    srch_yn, init_yn, new_yn, del_yn, save_yn,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
) VALUES (
    'SEB010', '사전검토관리', '/project/se/seb/SEB010',
    'Y', 'Y', 'Y', 'Y', 'N',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
);

-- ---------------------------------------------------------------------------
-- 2. sy_pgm_info 등록 (SEB011 - 사전검토등록, 서브화면)
-- ---------------------------------------------------------------------------
INSERT INTO sy_pgm_info (
    pgm_id, pgm_nm, pgm_path,
    srch_yn, init_yn, new_yn, del_yn, save_yn,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
) VALUES (
    'SEB011', '사전검토등록', '/project/se/seb/SEB011',
    'N', 'N', 'N', 'Y', 'Y',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
);

-- ---------------------------------------------------------------------------
-- 3. sy_pgm_info 등록 (SEB012 - 사전검토의견서등록내용, 서브화면)
-- ---------------------------------------------------------------------------
INSERT INTO sy_pgm_info (
    pgm_id, pgm_nm, pgm_path,
    srch_yn, init_yn, new_yn, del_yn, save_yn,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
) VALUES (
    'SEB012', '사전검토의견서등록내용', '/project/se/seb/SEB012',
    'N', 'N', 'N', 'N', 'Y',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
);

-- ---------------------------------------------------------------------------
-- 4. sy_menu_info 등록 (SEB010만 - 메인화면만 메뉴 등록)
-- ---------------------------------------------------------------------------
-- TODO: uppr_menu_id 값은 SE/SEB 카테고리의 상위 메뉴 ID로 확인 후 설정 필요
WITH new_id AS (
    SELECT COALESCE(MAX(menu_id)::int, 0) + 1 AS next_id FROM sy_menu_info
)
INSERT INTO sy_menu_info (
    menu_id, menu_nm, pgm_id, uppr_menu_id,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
)
SELECT
    next_id, '사전검토관리', 'SEB010', NULL, -- TODO: NULL -> 실제 상위 메뉴 ID로 교체
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
FROM new_id;
