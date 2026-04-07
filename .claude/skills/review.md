# review skill

## 역할
build command의 첫 번째 단계로 호출되는 **실행자**.
original vs final people_spec을 비교하여 변경분을 파악하고, final/machine_spec.yml을 생성.

## 입력
- `work/task/2.Working/original/people_spec.md`
- `work/task/2.Working/final/people_spec.md`
- `work/task/2.Working/original/machine_spec.yml`

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/config/naming.yml` | 네이밍 규칙으로 machine_spec 정규화 |
| `system/policies/framework/template_selection.yml` | skeleton_choice 재결정 |
| `system/policies/runtime/lifecycle.yml` | unresolved 정책 (build 시 차단) |
| `system/schemas/machine_spec.schema.json` | 구조 검증 |
| `system/templates/screen-types/*.yml` | 화면유형 정의 |

## 수행 절차

### 1. Final People Spec 검증
frontmatter(screen_id, screen_name, screen_type) 존재 확인.
필수 섹션 존재 확인.

### 2. Diff 분석
original vs final 차이 분석 → diff_summary 생성.
분석 대상: 검색 필드, 그리드 컬럼, 상세 필드, 버튼, 비즈니스 규칙, 화면유형.

### 3. Final Machine Spec 생성
diff_summary를 original/machine_spec.yml에 반영.
- 화면유형 변경 → `template_selection.yml` 기준 skeleton_choice 재결정
- 필드 변경 → placeholders 업데이트
- `naming.yml` 규칙으로 정규화

### 4. Schema 검증
`machine_spec.schema.json` 으로 구조 검증.
unresolved 존재 → `lifecycle.yml` → `unresolved_policy.build` 적용 (차단).

### 5. 저장 + 결과 반환
```yaml
review_result:
  status: "completed" | "failed"
  diff_summary: { changed_search_fields: N, ... }
  machine_spec_final_path: "work/task/2.Working/final/machine_spec.yml"
  warnings: []
  unresolved: []
```

## 실패 조건
frontmatter 불량, schema 검증 실패, unresolved 존재.
