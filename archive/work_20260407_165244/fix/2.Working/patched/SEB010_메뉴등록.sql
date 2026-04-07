-- =============================================================================
-- SEB010/SEB011/SEB012 프로그램 및 메뉴 등록 SQL (Fix 적용)
-- 대상 테이블: sy_pgm_info, sy_menu_info
-- Fix: menu_mdul_id 등 누락 컬럼 보완, 실제 DDL 정합성 확보
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. sy_pgm_info 등록 (SEB010 - 사전검토관리, 메인화면)
-- ---------------------------------------------------------------------------
INSERT INTO sy_pgm_info (
    pgm_id, pgm_nm, wrk_tp_cd, pgm_ctg_cd, pgm_path, use_yn,
    srch_yn, new_yn, save_yn, del_yn, prnt_yn, upld_yn, init_yn,
    menu_mdul_tp_id, menu_mdul_id,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
) VALUES (
    'SEB010', '사전검토관리', 'SE', 'SEB', '/se/seb/seb010', 'Y',
    'Y', 'Y', 'N', 'Y', 'N', 'N', 'Y',
    'SEB', 'SE',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
)
ON CONFLICT (pgm_id) DO UPDATE SET
    pgm_nm = excluded.pgm_nm,
    wrk_tp_cd = excluded.wrk_tp_cd,
    pgm_ctg_cd = excluded.pgm_ctg_cd,
    pgm_path = excluded.pgm_path,
    use_yn = excluded.use_yn,
    srch_yn = excluded.srch_yn,
    new_yn = excluded.new_yn,
    save_yn = excluded.save_yn,
    del_yn = excluded.del_yn,
    prnt_yn = excluded.prnt_yn,
    upld_yn = excluded.upld_yn,
    init_yn = excluded.init_yn,
    menu_mdul_tp_id = excluded.menu_mdul_tp_id,
    menu_mdul_id = excluded.menu_mdul_id,
    fina_reg_pgm_id = excluded.firs_reg_pgm_id,
    fina_reg_dts = now(),
    fina_reg_user_id = excluded.firs_reg_user_id,
    fina_reg_ip = excluded.firs_reg_ip;

-- ---------------------------------------------------------------------------
-- 2. sy_pgm_info 등록 (SEB011 - 사전검토등록, 서브화면)
-- ---------------------------------------------------------------------------
INSERT INTO sy_pgm_info (
    pgm_id, pgm_nm, wrk_tp_cd, pgm_ctg_cd, pgm_path, use_yn,
    srch_yn, new_yn, save_yn, del_yn, prnt_yn, upld_yn, init_yn,
    menu_mdul_tp_id, menu_mdul_id,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
) VALUES (
    'SEB011', '사전검토등록', 'SE', 'SEB', '/se/seb/seb011', 'Y',
    'N', 'N', 'Y', 'Y', 'N', 'N', 'N',
    'SEB', 'SE',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
)
ON CONFLICT (pgm_id) DO UPDATE SET
    pgm_nm = excluded.pgm_nm,
    wrk_tp_cd = excluded.wrk_tp_cd,
    pgm_ctg_cd = excluded.pgm_ctg_cd,
    pgm_path = excluded.pgm_path,
    use_yn = excluded.use_yn,
    srch_yn = excluded.srch_yn,
    new_yn = excluded.new_yn,
    save_yn = excluded.save_yn,
    del_yn = excluded.del_yn,
    prnt_yn = excluded.prnt_yn,
    upld_yn = excluded.upld_yn,
    init_yn = excluded.init_yn,
    menu_mdul_tp_id = excluded.menu_mdul_tp_id,
    menu_mdul_id = excluded.menu_mdul_id,
    fina_reg_pgm_id = excluded.firs_reg_pgm_id,
    fina_reg_dts = now(),
    fina_reg_user_id = excluded.firs_reg_user_id,
    fina_reg_ip = excluded.firs_reg_ip;

-- ---------------------------------------------------------------------------
-- 3. sy_pgm_info 등록 (SEB012 - 사전검토의견서등록내용, 서브화면)
-- ---------------------------------------------------------------------------
INSERT INTO sy_pgm_info (
    pgm_id, pgm_nm, wrk_tp_cd, pgm_ctg_cd, pgm_path, use_yn,
    srch_yn, new_yn, save_yn, del_yn, prnt_yn, upld_yn, init_yn,
    menu_mdul_tp_id, menu_mdul_id,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
) VALUES (
    'SEB012', '사전검토의견서등록내용', 'SE', 'SEB', '/se/seb/seb012', 'Y',
    'N', 'N', 'Y', 'N', 'N', 'N', 'N',
    'SEB', 'SE',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
)
ON CONFLICT (pgm_id) DO UPDATE SET
    pgm_nm = excluded.pgm_nm,
    wrk_tp_cd = excluded.wrk_tp_cd,
    pgm_ctg_cd = excluded.pgm_ctg_cd,
    pgm_path = excluded.pgm_path,
    use_yn = excluded.use_yn,
    srch_yn = excluded.srch_yn,
    new_yn = excluded.new_yn,
    save_yn = excluded.save_yn,
    del_yn = excluded.del_yn,
    prnt_yn = excluded.prnt_yn,
    upld_yn = excluded.upld_yn,
    init_yn = excluded.init_yn,
    menu_mdul_tp_id = excluded.menu_mdul_tp_id,
    menu_mdul_id = excluded.menu_mdul_id,
    fina_reg_pgm_id = excluded.firs_reg_pgm_id,
    fina_reg_dts = now(),
    fina_reg_user_id = excluded.firs_reg_user_id,
    fina_reg_ip = excluded.firs_reg_ip;

-- ---------------------------------------------------------------------------
-- 4. sy_menu_info 등록 (SEB010만 - 메인화면만 메뉴 등록)
-- menu_mdul_id = 'SE', uppr_menu_id = '5' (사전검토 폴더)
-- ---------------------------------------------------------------------------
INSERT INTO sy_menu_info (
    menu_mdul_id, menu_id, menu_nm, uppr_menu_id, menu_tp_cd, pgm_id,
    firs_reg_pgm_id, firs_reg_dts, firs_reg_user_id, firs_reg_ip,
    fina_reg_pgm_id, fina_reg_dts, fina_reg_user_id, fina_reg_ip
)
SELECT
    'SE',
    COALESCE(MAX(menu_id::int), 0) + 1,
    '사전검토관리', '5', 'P', 'SEB010',
    'SEB010', now(), 'SYSTEM', '127.0.0.1',
    'SEB010', now(), 'SYSTEM', '127.0.0.1'
FROM sy_menu_info
WHERE menu_mdul_id = 'SE'
  AND NOT EXISTS (
    SELECT 1 FROM sy_menu_info WHERE menu_mdul_id = 'SE' AND pgm_id = 'SEB010'
  );

-- 기존 레코드 업데이트 (이미 등록된 경우)
UPDATE sy_menu_info
SET menu_mdul_id = 'SE',
    menu_nm = '사전검토관리',
    uppr_menu_id = '5',
    menu_tp_cd = 'P',
    fina_reg_pgm_id = 'SEB010',
    fina_reg_dts = now(),
    fina_reg_user_id = 'SYSTEM',
    fina_reg_ip = '127.0.0.1'
WHERE pgm_id = 'SEB010';
