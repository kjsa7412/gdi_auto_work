# /analyze command

## 목적
work/task/1.Prep에 배치된 PPT 파일을 분석하여 people_spec(original), machine_spec(original), people_spec(final)을 생성한다. 코드를 생성하지 않는다.

## 실행 전 확인

1. **Lock 확인**: `work/.lock` 파일을 읽어 `UNLOCKED`인지 확인. `LOCKED:*`이면 즉시 중단.
2. **Active Context 확인**: `work/.active_context.yml`의 `current.phase`가 `idle` 또는 `task_prepared`인지 확인. 다른 상태이면 즉시 중단.
3. **입력 파일 확인**: `work/task/1.Prep/`에서 `.ppt` 또는 `.pptx` 파일 검색. 없으면 즉시 중단.
4. **금지 확인**: `archive/**`, `work/fix/**`를 입력으로 사용하지 않는다.

## 필수 참조 파일 (읽기 전용)

- `system/config/paths.yml`
- `system/config/framework_manifest.yml`
- `system/config/naming.yml`
- `system/policies/framework/template_selection.yml`
- `system/templates/screen-types/*.yml`
- `system/templates/spec/people_spec.md`
- `system/templates/spec/machine_spec.yml`
- `system/schemas/analyze_manifest.schema.json`
- `system/schemas/people_spec.schema.json`
- `system/schemas/machine_spec.schema.json`
- `system/cache/convention/**`
- `system/policies/runtime/allowed_paths.yml`

## 수행 절차

### 1. Workspace 초기화
- `work/.lock` ← `LOCKED:task:analyze`
- `work/.active_context.yml` ← current.type: task, current.phase: task_prepared, current.command: analyze, current.started_at: {now}

### 2. analyze.manifest 초기화
`work/task/2.Working/manifests/analyze.manifest.yml` 생성 (status: in_progress, inputs.prep_files: [발견된 파일들])

### 3. PPT 분석 및 화면 추출
1. PPT 파일을 읽어 슬라이드별 내용 추출 → `work/task/2.Working/extracted/`
2. 화면 후보 식별
3. screen_type 분류 (list/list-detail/form/popup) — `system/templates/screen-types/*.yml` 기준
4. 분류 결과 → `work/task/2.Working/classified/`

### 4. people_spec(original) 생성
- `system/templates/spec/people_spec.md` 템플릿 기반
- PPT에서 추출한 화면 개요, 조회 조건, 그리드 컬럼, 버튼, 비즈니스 규칙 채움
- 미확인 항목은 "미확정 항목" 섹션 기록
- 저장: `work/task/2.Working/original/people_spec.md`

### 5. machine_spec(original) 생성
- `system/templates/spec/machine_spec.yml` 템플릿 기반
- naming.yml 규칙에 따라 screen_id, module_id, mapper_namespace 결정
- template_selection.yml에 따라 skeleton_choice 결정
- PPT에서 확인 가능한 placeholder 채움, 불가 항목은 unresolved 기록
- `system/schemas/machine_spec.schema.json`으로 검증
- 저장: `work/task/2.Working/original/machine_spec.yml`

### 6. people_spec(final) 초기 복사
`original/people_spec.md` → `final/people_spec.md` 복사. **이 파일이 사용자 검토/수정 대상**.

### 7. analyze.manifest 완료
status: completed, outputs 경로, screens 목록, unresolved, completed_at 기록

### 8. Active Context 갱신
- current.phase: task_analyzed, task.status: analyzed, task.last_analyze_at: {now}
- spec 경로들 기록
- `work/.lock` ← `UNLOCKED`

### 9. 결과 보고
- 추출 화면 수, screen_type 분류 결과, unresolved 목록
- **사용자 검토 대상: `work/task/2.Working/final/people_spec.md`**
- 검토 완료 후 `/build` 실행 가능 안내

## 실패 조건
- Prep에 PPT 없음, Lock 충돌, 화면 식별 실패, spec 생성 실패 → failed 상태, lock 해제

## 성공 조건
- original/people_spec.md + original/machine_spec.yml + final/people_spec.md + analyze.manifest.yml 존재
- active_context.current.phase == task_analyzed
