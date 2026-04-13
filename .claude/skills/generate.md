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

### 4-1. 컬럼명 실재 검증 (CR-001 Cross-Cutting, CRITICAL)
**machine_spec의 모든 field_id, column_id를 `system/cache/convention/table.txt`에서 대조 검증한다.**

절차:
1. machine_spec에서 `placeholders.search_fields[].field_id`, `placeholders.grid_columns[].column_id`,
   `placeholders.detail_fields[].field_id`, `sql.main_table`, `sql.detail_table` 을 수집한다.
2. 각 field_id/column_id가 해당 테이블(main_table/detail_table)의 실제 컬럼으로 존재하는지 table.txt에서 확인한다.
3. **불일치 발견 시 즉시 생성을 중단하고, table.txt에서 올바른 컬럼명을 찾아 machine_spec을 교정한 후 재시작한다.**
4. table.txt에도 없고 DB fallback으로도 없으면 unresolved 처리한다 (에이전트가 추론 생성하는 것을 절대 금지).

이 검증은 analyze 시점의 CR-001과 동일한 절차이며, **generate 시점에서 한 번 더 수행하는 이중 관문**이다.
컬럼명이 추가/변경되는 모든 경로(analyze, build review, fix, 수동 보강)에서 이 검증을 통과해야 한다.

참조: `system/policies/analyze/default_resolution.yml` → `column_resolution (CR-001)`

### 5. Placeholder 치환
skeleton 파일의 `{{placeholder}}` 를 machine_spec 값으로 치환.
- `code_component_defaults.yml` 규칙으로 코드콤보/코드헬프 생성
- `button_mapping.yml` 규칙으로 버튼 → listener 매핑

### 5-1. 커스텀 로직 블록 생성 (CRITICAL)
skeleton의 기본 placeholder(`search_fields`, `grid_columns` 등) 외에 비즈니스 로직이 필요한 경우,
다음 OPTIONAL placeholder 블록에 코드를 생성한다:
- `{{custom_button_handlers}}` — 커스텀 버튼 핸들러 (etc1, etc2, del 등)
- `{{active_pgm_handler}}` — 서브화면 진입(activePgm) + loadDetail
- `{{dynamic_form_control}}` — 동적 폼 필드 show/hide/readonly 전환
- `{{grid_events}}` — 그리드 행 클릭/더블클릭 이벤트

**[절대 규칙] skeleton 밖 코드 작성 시에도 반드시 skeleton 내 API 패턴을 준수해야 한다.**

금지 API와 올바른 API 대응표:

| 금지 (존재하지 않는 API) | 올바른 API |
|---|---|
| `platform.alert(msg)` | `popup.alert.show(msg)` |
| `platform.confirm(msg, fn)` | `popup.confirm.show(msg, function(confirmYn) { if (confirmYn) {...} })` |
| `platform.dataSearchForm(id, config)` | `listener.form.searchForm = webix.ui({container, view:'dataForm', pgm:PGM, search:true, elements})` |
| `platform.dataForm(id, config)` | `listener.form.xxx = webix.ui({container, view:'dataForm', pgm:PGM, elements})` |
| `platform.dataGrid(id, config)` | `listener.grid.xxx = webix.ui({container, view:'datagrid', listener, columns})` |
| `platform.dataToolbar(id, config)` | `listener.toolbar.xxx = webix.ui({container, view:'dataToolbar', pgm:PGM, title})` |
| `platform.getSearchFormParam(id)` | `listener.form.searchForm.getData()` |
| `platform.getFormValues(id)` | `listener.form.xxx.getData()` |
| `platform.setFormValues(id, data)` | `listener.form.xxx.setData(data)` |
| `platform.setFormReadonly(id, flag)` | `listener.form.xxx.setReadonly(flag)` |
| `platform.resetSearchForm(id)` | `listener.form.searchForm.init()` |
| `platform.setGridData(id, data)` | `listener.grid.xxx.setData(data)` |
| `platform.getGridData(id)` | `listener.grid.xxx.getData()` |
| `platform.getCheckedGridData(id)` | `listener.grid.xxx.getCheckedData()` |
| `platform.getSelectedGridData(id)` | `listener.grid.xxx.getSelectedItem()` |
| `platform.setFormFieldVisible(id, fields, flag)` | `listener.form.xxx.getView(field).show() / .hide()` |
| `platform.setFormFieldReadonly(id, field, flag)` | `listener.form.xxx.getView(field).config.readonly=flag; .refresh()` |
| `platform.openPopup(id, config)` | `new DataModalNew() + DataModalConfig` |
| `function(data){...}` (POST 콜백) | `new Callback(function(result) { if (result.resultCode === RESULT_CODE.OK) {...} })` |

**자유 생성 판단 기준:**
- skeleton placeholder로 치환 가능한 부분 → 반드시 skeleton 치환
- skeleton에 없는 비즈니스 로직 → OPTIONAL 블록에 작성하되, `html_patterns.yml`의 event_patterns/popup/platform_api 섹션에 정의된 API 패턴만 사용
- 패턴이 불확실한 경우 → `framework_manifest.yml`의 `framework_source_fallback` 절차에 따라 실제 소스에서 검증 후 적용
- **에이전트가 추론으로 존재하지 않는 편의 함수를 만들어 사용하는 것을 절대 금지**

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

**[CRITICAL] HC-006 실질적 검증 (hallucinated API 탐지):**
생성된 각 HTML 파일에 대해 다음 정규식으로 실제 내용을 검색한다:
```
/platform\.(?!post|url|listener)[a-zA-Z]+\(/
```
이 정규식에 매치되는 패턴이 하나라도 발견되면 **critical fail**로 판정한다.
형식적으로 pass를 기록하는 것을 금지하며, 반드시 파일을 읽어서 검증해야 한다.

구체적 탐지 대상: `platform.alert(`, `platform.confirm(`, `platform.dataSearchForm(`,
`platform.dataForm(`, `platform.dataGrid(`, `platform.dataToolbar(`,
`platform.getSearchFormParam(`, `platform.getFormValues(`, `platform.setFormValues(`,
`platform.setGridData(`, `platform.getCheckedGridData(`, `platform.resetSearchForm(` 등

발견 시 조치: `forbidden_apis.yml` → `hallucinated_api_forbidden` 섹션의 correct 패턴으로 자동 수정 후 재검증.

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
