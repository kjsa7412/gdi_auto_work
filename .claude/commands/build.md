# /build command

## 역할
사용자 검토 완료된 people_spec을 기반으로 machine_spec을 확정하고, skeleton 기반 코드 산출물을 생성하는 **오케스트레이터**.

## 진입 조건
`system/config/runtime.yml` → `commands.build.start_condition` 참조.
- phase: task_analyzed
- lock: UNLOCKED
- 필수 입력: `runtime.yml` → `commands.build.inputs.required` 참조
- 금지: `archive/**` 입력, skeleton 미정의 artifact 직접 작성

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/config/runtime.yml` | 진입 조건, skill 호출 순서, 상태 전이 |
| `system/policies/runtime/db_access.yml` | cache 검증, DB fallback |
| `system/policies/runtime/allowed_paths.yml` | 경로 접근 제어 |
| `system/policies/runtime/context_isolation.yml` | lock, 격리 |
| `system/policies/runtime/lifecycle.yml` | 필수 산출물, unresolved 정책 |
| `system/policies/runtime/deploy_policy.yml` | deploy 진입 조건, 모드, 실패 영향 |
| `system/policies/framework/*.yml` | 전체 framework 정책 (generate skill에 전달) |
| `system/policies/verify/*.yml` | 전체 verify 정책 (generate skill에 전달) |

## 수행 절차

### 1. Lock 획득 + Manifest 초기화
`context_isolation.yml` lock 형식: `LOCKED:task:build`

### 2. Review Skill 호출
`runtime.yml` → `commands.build.skills_used[0]`: review
- 입력: original/final people_spec, original machine_spec
- 반환: final/machine_spec.yml, diff_summary, unresolved
- unresolved 존재 시 → `lifecycle.yml` → `unresolved_policy.build` 적용 (차단)

### 3. Cache 검증 및 DB Fallback
`db_access.yml` → `db_fallback` 정책에 따라 cache 검증.
결과를 build.manifest.cache 섹션에 기록.

### 4. Generate Skill 호출
`runtime.yml` → `commands.build.skills_used[1]`: generate
- 입력: final/machine_spec.yml + 전체 framework/verify 정책
- 반환: generated_paths, verify_details, self_diagnosis, critical_pass, manual_required, auto_fixed

### 5. Verify Result 생성
generate skill 반환 기반으로 `verify_result.yml` 생성.
`verify_result.schema.json` 기준 구조.

### 6. 위반 처리
`escalation_rules.yml` 정책에 따라:
- critical + auto_fixable → 자동 수정 후 재검증
- critical + manual_required → build failed
- warnings → 경고 기록 후 계속

### 7. Deliverables + Report 정리
generated → deliverables, build_report.md 생성.

### 8. Deploy Skill 자동 호출
`deploy_policy.yml` → `entry_conditions.from_build` 충족 시 **deploy skill 자동 호출**.
- 대상: `work/task/3.Result/deliverables/**`
- 세부 절차는 `skills/deploy.md` 참조
- deploy 실패는 build 실패가 아님

### 9. Manifest 완료 + Active Context 갱신 + Lock 해제
`runtime.yml` → `commands.build.success_state`: task_built

### 10. 결과 보고
review diff, 생성 파일, verify 요약, 자기진단 요약, deploy 배포 결과.

## 실패/성공 조건
`runtime.yml` → `commands.build` 및 `lifecycle.yml` → `mandatory_output_check.build` 참조.
성공: critical_pass=true, deliverables 배치 완료, build.manifest status=completed.
deploy 실패는 build 실패가 아님.
