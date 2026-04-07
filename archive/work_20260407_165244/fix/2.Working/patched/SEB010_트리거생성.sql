-- =============================================================================
-- 사전검토의견서 채택결과 등록 트리거
-- 대상 테이블: rp_pre_rview_opl_list
-- 근거: SEB010 PPT [데이터] 섹션
-- =============================================================================
-- 로직:
--   1. rp_pre_rview_opl_list.adpt_rslt_cd가 INSERT/UPDATE 되면
--   2. 해당 wrk_mast_id의 사전검토마스터가 사전검토완료(030) 상태가 아닌지 확인
--   3. 해당 wrk_mast_id에 연결된 모든 의견서의 adpt_rslt_cd가 NOT NULL인지 확인
--   4. 모두 등록되었으면:
--      - rp_pre_rview_mst.opl_wrt_dt = 오늘 날짜 (yyyymmdd)
--      - rp_pre_rview_mst.prgrs_stat_cd = '030' (사전검토완료)
-- =============================================================================

-- 1. 트리거 함수 생성
CREATE OR REPLACE FUNCTION fn_seb_check_opl_complete()
RETURNS TRIGGER AS $$
DECLARE
    v_master_stat VARCHAR(10);
    v_total_cnt   INT;
    v_done_cnt    INT;
BEGIN
    -- 현재 사전검토마스터의 진행상태 조회
    SELECT prgrs_stat_cd
      INTO v_master_stat
      FROM rp_pre_rview_mst
     WHERE wrk_mast_id = NEW.wrk_mast_id;

    -- 이미 사전검토완료(030) 상태이면 아무것도 하지 않음
    IF v_master_stat = '030' THEN
        RETURN NEW;
    END IF;

    -- 해당 사전검토에 연결된 전체 의견서 수
    SELECT COUNT(*)
      INTO v_total_cnt
      FROM rp_pre_rview_opl_list
     WHERE wrk_mast_id = NEW.wrk_mast_id;

    -- 채택결과가 등록된 의견서 수 (adpt_rslt_cd IS NOT NULL AND != '')
    SELECT COUNT(*)
      INTO v_done_cnt
      FROM rp_pre_rview_opl_list
     WHERE wrk_mast_id = NEW.wrk_mast_id
       AND adpt_rslt_cd IS NOT NULL
       AND adpt_rslt_cd != '';

    -- 전체 의견서의 채택결과가 모두 등록되었으면
    IF v_total_cnt > 0 AND v_total_cnt = v_done_cnt THEN
        UPDATE rp_pre_rview_mst
           SET opl_wrt_dt = TO_CHAR(NOW(), 'YYYYMMDD'),
               prgrs_stat_cd = '030',
               fina_reg_dts = NOW(),
               fina_reg_pgm_id = 'SEB012',
               fina_reg_user_id = 'TRIGGER'
         WHERE wrk_mast_id = NEW.wrk_mast_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. 트리거 생성 (INSERT OR UPDATE 시 실행)
DROP TRIGGER IF EXISTS trg_seb_check_opl_complete ON rp_pre_rview_opl_list;

CREATE TRIGGER trg_seb_check_opl_complete
    AFTER INSERT OR UPDATE OF adpt_rslt_cd
    ON rp_pre_rview_opl_list
    FOR EACH ROW
    EXECUTE FUNCTION fn_seb_check_opl_complete();
