# review skill

## 목적
build command의 첫 번째 단계로 호출된다. original people_spec과 final people_spec을 비교하여 변경/추가 사항을 파악하고, 이를 original/machine_spec.yml에 반영하여 **final/machine_spec.yml을 생성**하는 것이 최종 목표이다. 이후 모든 단계는 final/machine_spec.yml만을 기준으로 진행한다.

## 입력 (build command로부터)

- `work/task/2.Working/original/people_spec.md` — 원본 people_spec
- `work/task/2.Working/final/people_spec.md` — 사용자 검토/수정 완료 people_spec
- `work/task/2.Working/original/machine_spec.yml` — 원본 machine_spec
- `system/config/naming.yml` — 네이밍 규칙
- `system/policies/framework/template_selection.yml` — skeleton 선택 규칙
- `system/templates/screen-types/*.yml` — 화면유형 정의
- `system/schemas/machine_spec.schema.json` — machine_spec 검증

## 수행 절차

### 1. Final People Spec 검증
`work/task/2.Working/final/people_spec.md`를 읽고:
- YAML frontmatter(meta 블록)에 screen_id, screen_name, screen_type 존재 확인
- 필수 섹션(화면 개요, 조회 조건, 목록/그리드, 버튼 및 기능) 존재 확인
- "미확정 항목" 섹션의 미해결 TODO가 있으면 경고 반환 (차단하지는 않음)

### 2. Original vs Final 비교 분석
`original/people_spec.md`와 `final/people_spec.md`의 차이를 분석한다:
- **조회 조건**: 추가/삭제/변경된 검색 필드
- **그리드 컬럼**: 추가/삭제/변경된 컬럼, 순서 변경
- **상세 필드**: 추가/삭제/변경된 입력 필드
- **버튼/기능**: 추가/삭제된 버튼, 동작 변경
- **비즈니스 규칙**: 추가/변경된 검증/계산 규칙
- **화면 유형**: screen_type 변경 여부 (변경 시 skeleton_choice도 재결정)

변경 요약을 `diff_summary` 객체로 정리한다.

### 3. Final Machine Spec 생성
`original/machine_spec.yml`을 기반으로 diff_summary의 변경 사항을 반영하여 `final/machine_spec.yml`을 생성한다:

- **screen_type 변경 시**: skeleton_choice 재결정 (template_selection.yml 기준)
- **검색 필드 변경 시**: placeholders.search_fields 배열 업데이트
- **그리드 컬럼 변경 시**: placeholders.grid_columns 배열 업데이트
- **상세 필드 변경 시**: placeholders.detail_fields 배열 업데이트
- **버튼 변경 시**: placeholders.action_buttons 업데이트
- **SQL 관련 변경 시**: sql.select_sql_id, sql.insert_sql_id 등 업데이트, insert_columns/insert_values 재계산
- **controller/service 필요 여부 재판단**: 비즈니스 규칙 복잡도에 따라

machine_spec의 모든 필드를 `naming.yml` 규칙에 따라 정규화한다.

### 4. Machine Spec 검증
- `system/schemas/machine_spec.schema.json`으로 구조 검증
- **unresolved 항목 존재 시**: 즉시 실패 반환 — build command에서 사용자에게 안내하도록 위임
- skeleton_choice가 실제 존재하는 skeleton 파일과 일치하는지 확인

### 5. 저장
- 저장: `work/task/2.Working/final/machine_spec.yml`

### 6. 결과 반환
build command에 반환:
```yaml
review_result:
  status: "completed"  # 또는 "failed"
  diff_summary:
    changed_search_fields: 2
    changed_grid_columns: 3
    changed_detail_fields: 0
    changed_buttons: 1
    screen_type_changed: false
    total_changes: 6
  machine_spec_final_path: "work/task/2.Working/final/machine_spec.yml"
  warnings: []      # 미확정 항목 경고 등
  unresolved: []    # 미해결 항목 (있으면 build 차단)
```

## 실패 조건
- `final/people_spec.md` 없음 또는 frontmatter 불량
- `original/machine_spec.yml` 없음
- machine_spec.schema.json 검증 실패
- unresolved 항목 존재

## 성공 조건
- `final/machine_spec.yml` 생성 + schema 검증 통과
- unresolved 없음
- diff_summary 완성
