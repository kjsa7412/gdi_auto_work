-- =============================================================================
-- code_select.sql
-- 공통코드 정의 캐시 수집용 SQL (조회 전용)
-- DBMS: PostgreSQL
-- 근거: sy_code_mst, sy_code_dtl 테이블 (commonMapper.xml, dta010.xml 등에서 확인)
-- =============================================================================

-- 코드 마스터 조회 (코드유형 목록)
SELECT
    scm.comp_cd,
    scm.wrk_tp_cd,
    scm.cd_tp_cd,
    scm.cd_tp_nm,
    scm.use_yn,
    scm.rmk
FROM sy_code_mst scm
WHERE scm.use_yn = 'Y'
ORDER BY scm.comp_cd, scm.wrk_tp_cd, scm.cd_tp_cd;

-- 코드 상세 조회 (코드값 목록)
SELECT
    scd.comp_cd,
    scd.wrk_tp_cd,
    scd.cd_tp_cd,
    scd.cd,
    scd.cd_nm,
    scd.cd_rmk,
    scd.sort_ord,
    scd.use_yn
FROM sy_code_dtl scd
WHERE scd.use_yn = 'Y'
ORDER BY scd.comp_cd, scd.wrk_tp_cd, scd.cd_tp_cd, scd.sort_ord, scd.cd;

-- TODO: comp_cd 필터 조건 추가 필요 (실제 운영 시 특정 회사 코드로 제한)
-- TODO: wrk_tp_cd 목록 확인 필요 (PS, DT, SY 등 업무구분 코드)
