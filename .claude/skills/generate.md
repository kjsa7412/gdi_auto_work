# generate skill

## 목적
build command가 호출하는 코드 생성 skill. machine_spec(final)을 읽고 skeleton 기반으로 html/xml/controller/service artifact를 생성한다.

**절대 규칙: skeleton 파일을 직접 사용하여 placeholder를 치환한다. 자유 생성 금지.**

## 입력 (build command로부터)

- `work/task/2.Working/final/machine_spec.yml` — 생성 대상 스펙
- `system/policies/framework/template_selection.yml` — skeleton 선택 규칙
- `system/policies/framework/skeleton_contract.yml` — placeholder 계약
- `system/templates/screen-types/*.yml` — 화면유형 정의
- `system/templates/skeletons/**` — skeleton 파일들
- `system/policies/framework/forbidden_apis.yml` — 금지 패턴
- `system/policies/framework/html_patterns.yml` — HTML 규칙
- `system/policies/framework/xml_patterns.yml` — XML 규칙
- `system/policies/framework/controller_patterns.yml` — Controller 규칙
- `system/policies/framework/service_patterns.yml` — Service 규칙
- `system/config/naming.yml` — 네이밍 규칙

## 수행 절차

### 1. Machine Spec 읽기
`final/machine_spec.yml`을 읽고 screen_type, skeleton_choice, placeholders, sql, artifacts 구조를 파싱한다.

### 2. Screen Type 확인
machine_spec의 screen_type이 `system/templates/screen-types/{type}.yml`에 정의된 유효 유형인지 확인. 없으면 실패.

### 3. Skeleton 조합 결정
`template_selection.yml`의 규칙에 따라:
- 필수 skeleton (html, xml) 경로 확정
- 선택 skeleton (controller, service) 필요 여부 결정
- machine_spec.skeleton_choice와 일치 확인

### 4. Placeholder Completeness 확인
`skeleton_contract.yml`의 `required_matrix.{screen_type}`에 따라:
- 각 아티팩트별 필수 placeholder가 machine_spec.placeholders에 모두 존재하는지 확인
- 누락 시 **즉시 실패** — 누락 항목 목록 반환

### 5. Placeholder 치환
각 skeleton 파일을 읽어 `{{placeholder_name}}` 형식의 placeholder를 machine_spec 값으로 치환한다.
- 단일값 placeholder: 문자열 직접 치환
- 배열형 placeholder (search_fields, grid_columns 등): machine_spec의 배열 데이터를 해당 프레임워크 코드 블록으로 변환
  - search_fields → webix.ui dataForm elements 코드 블록
  - grid_columns → webix.ui datagrid columns 코드 블록
  - insert_columns, insert_values → SQL INSERT 절 코드 블록
- OPTIONAL 블록: machine_spec에 해당 기능이 없으면 주석 처리 또는 제거

### 6. Artifact 생성
치환된 파일을 `work/task/2.Working/generated/`에 저장:
- HTML: `{SCREEN_ID}.html`
- XML: `{module_id}.xml`
- Controller: `{ModuleId}Controller.java` (필요 시)
- Service: `{ModuleId}Service.java` (필요 시)

### 7. Self-check 수행
생성된 파일에 대해:
- `{{` 잔존 placeholder 검출 → error
- forbidden_apis.yml 패턴 매칭 → error
- naming.yml 규칙 준수 확인 → warn/error
- XML: comp_cd 필터, audit 컬럼 포함 확인 → error
- HTML: PGM prefix, fragment include, IIFE 패턴 확인 → error

### 8. 결과 반환
build command에 반환:
```yaml
generated_paths: ["work/task/2.Working/generated/DTA030.html", ...]
self_check:
  - id: "placeholder_complete"
    result: "pass"
  - id: "forbidden_pattern"
    result: "pass"
violations: []
```

## 실패 조건
- 필수 placeholder 누락
- skeleton 파일 없음
- screen_type 불명확
- self-check에 error 존재

## 성공 조건
- artifact_plan에 맞는 파일 모두 생성
- generated_paths 목록 완성
- self_check에 error 없음 (warn은 허용)
