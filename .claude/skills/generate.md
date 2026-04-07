# generate skill

## 역할
build command가 호출하는 코드 생성 **실행자**.
machine_spec(final)을 읽고, 정책 파일 기반으로 skeleton 치환 → artifact 생성 → SQL 생성 → verify 수행.

## 입력
- `work/task/2.Working/final/machine_spec.yml`

## 필수 정책 로드

**Framework 정책 (생성 규칙):**
- `system/policies/framework/template_selection.yml` — skeleton 선택
- `system/policies/framework/skeleton_contract.yml` — placeholder 계약
- `system/policies/framework/forbidden_apis.yml` — 금지 패턴
- `system/policies/framework/html_patterns.yml` — HTML 패턴
- `system/policies/framework/xml_patterns.yml` — XML 패턴
- `system/policies/framework/controller_patterns.yml` — Controller 패턴
- `system/policies/framework/service_patterns.yml` — Service 패턴
- `system/policies/framework/layout_rules.yml` — 레이아웃
- `system/policies/framework/sql_generation.yml` — SQL 생성
- `system/policies/framework/code_registration.yml` — 코드등록
- `system/policies/framework/button_mapping.yml` — 버튼 매핑
- `system/policies/framework/code_component_defaults.yml` — 코드 컴포넌트
- `system/policies/framework/postgresql_rules.yml` — PostgreSQL 금지 패턴

**Verify 정책 (검증 규칙):**
- `system/policies/verify/html_checks.yml`
- `system/policies/verify/xml_checks.yml`
- `system/policies/verify/sql_checks.yml`
- `system/policies/verify/controller_checks.yml`
- `system/policies/verify/service_checks.yml`
- `system/policies/verify/self_diagnosis.yml`
- `system/policies/verify/escalation_rules.yml`

**기타:**
- `system/config/naming.yml`
- `system/templates/screen-types/*.yml`
- `system/templates/skeletons/**`

## 수행 절차

### 1. Machine Spec 읽기
screen_type, skeleton_choice, placeholders, sql, artifacts 파싱.

### 2. Screen Type 확인
`screen-types/{type}.yml` 유효 유형 확인. 없으면 실패.

### 3. Skeleton 조합 결정
`template_selection.yml` 규칙 적용.

### 4. Placeholder Completeness 확인
`skeleton_contract.yml` → `required_matrix.{screen_type}` 검증.
누락 시 즉시 실패.

### 5. Placeholder 치환
skeleton 파일의 `{{placeholder}}` 를 machine_spec 값으로 치환.
- `code_component_defaults.yml` 규칙으로 코드콤보/코드헬프 생성
- `button_mapping.yml` 규칙으로 버튼 → listener 매핑

### 6. Artifact 저장
`work/task/2.Working/generated/` 에 `naming.yml` 규칙 기준 파일명으로 저장.

### 7. SQL 산출물 생성
`sql_generation.yml` 규칙 적용:
- 메뉴등록 SQL (SG-001): 필수
- DDL보완 SQL (SG-002): `self_diagnosis.yml` SD-001 기준
- 코드등록 SQL (SG-003): `code_registration.yml` + `self_diagnosis.yml` SD-002 기준
- execution_order.md (SG-004): SQL 1개 이상 시 필수

### 8. Verify 수행
각 artifact에 해당 verify 정책 적용:
- HTML → `html_checks.yml` (HC-*)
- XML → `xml_checks.yml` (XC-*)
- SQL → `sql_checks.yml` (SC-*)
- Controller → `controller_checks.yml` (CC-*)
- Service → `service_checks.yml` (SVC-*)
- PostgreSQL → `postgresql_rules.yml` (PG-*)
- 자기진단 → `self_diagnosis.yml` (SD-*)
- Escalation → `escalation_rules.yml` (ER-*)

### 9. 결과 반환
```yaml
generated_paths: [...]
verify_details:
  html_checks: { passed: N, failed: N, warnings: N }
  xml_checks: { passed: N, failed: N, warnings: N }
  sql_checks: { passed: N, failed: N, warnings: N }
  controller_checks: { passed: N, failed: N, warnings: N }
  service_checks: { passed: N, failed: N, warnings: N }
self_diagnosis:
  ddl_supplements: []
  code_registrations: []
  auto_fixed: []
  manual_required: []
critical_pass: true|false
manual_required: true|false
auto_fixed: []
```

## 실패 조건
placeholder 누락, skeleton 없음, screen_type 불명, critical fail + 자동 수정 불가.

## 성공 조건
artifact 전체 생성, critical_pass=true (자동 수정 포함), warn 허용.
