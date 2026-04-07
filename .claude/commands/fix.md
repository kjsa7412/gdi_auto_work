# /fix command

## 역할
fix 요청을 분석하여 영향 범위를 판정하고, 패치/재생성/정책 보완을 수행하는 **오케스트레이터**.

## 진입 조건
`system/config/runtime.yml` → `commands.fix.start_condition` 참조.
- phase: task_built
- lock: UNLOCKED
- 입력: `work/fix/1.Prep/`에 fix 요청 파일 존재
- 금지: `archive/**` 입력, `work/task/**` 직접 수정

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/config/runtime.yml` | 진입 조건, 상태 전이 |
| `system/policies/runtime/db_access.yml` | cache 조회, DB fallback |
| `system/policies/runtime/allowed_paths.yml` | 경로 접근 (system-fix 쓰기 포함) |
| `system/policies/runtime/context_isolation.yml` | fix 격리, task 읽기전용 |
| `system/policies/runtime/lifecycle.yml` | 필수 산출물, unresolved 정책 |
| `system/policies/verify/issue_classification.yml` | 오류 유형 분류, 에러 코드 체계 |
| `system/policies/verify/escalation_rules.yml` | system-fix 트리거 조건 |
| `system/policies/verify/*.yml` | 전체 verify 정책 (오류 판정 근거) |
| `system/policies/framework/*.yml` | 전체 framework 정책 |

## 수행 절차

### 1. Lock 획득 + Manifest 초기화
`context_isolation.yml` lock 형식: `LOCKED:fix:fix`

### 2. Fix 요청 분석
`work/fix/1.Prep/` 파일 읽기 및 내용 분석.

### 3. 오류 유형 분류
`issue_classification.yml` → `issue_types` 기준으로 분류:
output_patch / skeleton_patch / reanalyze / rebuild / policy_patch

`issue_classification.yml` → `patch_distinction` 기준으로 단순 patch vs 정책 patch 판정.

### 4. Cache 기반 원인 조회
`db_access.yml` → `db_fallback` 정책에 따라 convention cache 조회 및 DB fallback.

### 5. 영향 범위 판정
reanalyze_required, rebuild_required, policy_patch_required, affected_paths 결정.

### 6. Verify 정책 기반 오류 판정
`issue_classification.yml` → `error_code_taxonomy` 기준으로 오류를 verify check ID에 매핑.

### 7. System-fix Skill 연계 판정
`escalation_rules.yml` → ER-004 기준으로 system-fix 자동 호출 여부 결정.
호출 시 system-fix skill에 오류 분류 결과 + severity + 영향 범위 전달.

### 8. 수정 수행
`issue_classification.yml` → issue_type에 따라:
- output_patch → patched/ → deliverables/
- rebuild → "/build 재실행 필요" 안내
- reanalyze → "/analyze + /build 재실행 필요" 안내
- skeleton_patch/policy_patch → system-fix 결과에 따라 처리

### 9. Verify Result 생성
fix 적용 후 재검증 결과를 `verify_result.yml`로 생성.

### 10. Deploy Skill 자동 호출
`deploy_policy.yml` → `entry_conditions.from_fix` 충족 시 **deploy skill 자동 호출**.
- 대상: `work/fix/3.Result/deliverables/**`
- 세부 절차는 `skills/deploy.md` 참조
- deploy 실패는 fix 실패가 아님

### 11. Manifest 완료 + Active Context 갱신 + Lock 해제
`runtime.yml` → `commands.fix.success_state`: fix_applied

### 12. 결과 보고
fix 유형, 영향 범위, 수행 조치, verify 결과 요약, deploy 배포 결과.

## 실패/성공 조건
`runtime.yml` → `commands.fix` 및 `lifecycle.yml` → `mandatory_output_check.fix` 참조.
