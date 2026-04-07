# /analyze command

## 역할
PPT를 분석하여 people_spec + machine_spec을 생성하는 **오케스트레이터**.
코드를 생성하지 않는다.

## 진입 조건
`system/config/runtime.yml` → `commands.analyze.start_condition` 참조.
- phase: idle 또는 task_prepared
- lock: UNLOCKED
- 입력: `work/task/1.Prep/`에 .ppt 또는 .pptx 존재
- 금지: `archive/**`, `work/fix/**` 입력 사용 금지

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/config/runtime.yml` | 진입 조건, 필수 산출물, 상태 전이 |
| `system/policies/runtime/db_access.yml` | DB 접속, cache-first 원칙, fallback 규칙 |
| `system/policies/runtime/allowed_paths.yml` | 읽기/쓰기 허용 경로 |
| `system/policies/runtime/context_isolation.yml` | task 격리, lock 형식 |
| `system/policies/runtime/lifecycle.yml` | 필수 산출물 검증, unresolved 정책 |
| `system/policies/analyze/ppt_extraction.yml` | PPT 추출 규칙 (PE-001~PE-006) |
| `system/policies/analyze/classify_tags.yml` | 13개 태그 분류 |
| `system/policies/analyze/screen_type_rules.yml` | 화면유형 판정 |
| `system/policies/analyze/default_resolution.yml` | 보완 우선순위, 태그 규칙 |
| `system/policies/analyze/spec_generation.yml` | spec 생성 규칙, 미확정 처리, cache 연계 |
| `system/config/naming.yml` | 네이밍 규칙 |
| `system/policies/framework/template_selection.yml` | skeleton 선택 |

## 수행 절차

### 1. Lock 획득 + Active Context 갱신
`context_isolation.yml` lock 형식에 따라 `LOCKED:task:analyze` 설정.

### 2. Convention Cache Refresh (필수)
`db_access.yml` 정책에 따라 cache refresh 수행.
- `db_access.yml` → `connection` 정보로 psql 실행
- `db_access.yml` → `cache_refresh` 규칙에 따라 무조건 1회 수행
- 실패 시: `db_access.yml` → `cache_refresh.failure_handling` 정책 적용

### 3. Cache 기반 정보 보강 및 DB Fallback
`db_access.yml` → `db_fallback` 정책에 따라 부족한 정보 보완.
결과를 analyze.manifest에 기록.

### 4. analyze.manifest 초기화
`lifecycle.yml` → `mandatory_output_check.analyze` 기준으로 필수 산출물 목록 확인.

### 5. PPT 분석 → 화면 추출 → 분류
`ppt_extraction.yml` (PE-001~PE-006) 규칙으로 PPT 추출.
`classify_tags.yml` 규칙으로 태그 분류.
`screen_type_rules.yml` 규칙으로 화면유형 판정.

### 6. people_spec(original) 생성
`spec_generation.yml` → `people_spec_rules` 적용.
`default_resolution.yml` → 보완 우선순위 및 태그 규칙 적용.

### 7. machine_spec(original) 생성
`spec_generation.yml` → `machine_spec_rules` 적용.
`naming.yml` 규칙으로 정규화.
`template_selection.yml` 규칙으로 skeleton_choice 결정.
`machine_spec.schema.json` 으로 검증.

### 8. people_spec(final) 초기 복사
`spec_generation.yml` → `final_people_spec` 규칙 적용.

### 9. Manifest 완료 + Active Context 갱신 + Lock 해제
`runtime.yml` → `commands.analyze.success_state`: task_analyzed

### 10. 결과 보고
추출 화면 수, screen_type, unresolved 목록 안내.
**사용자 검토 대상**: `work/task/2.Working/final/people_spec.md`

## 실패/성공 조건
`runtime.yml` → `commands.analyze` 및 `lifecycle.yml` → `mandatory_output_check.analyze` 참조.
