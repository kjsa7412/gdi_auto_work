# /build command

## 목적
people_spec과 machine_spec을 읽어 machine_spec(final)을 확정하고, skeleton 기반으로 코드 산출물을 생성한다.

**절대 규칙: skeleton 없는 자유 생성 금지. skeleton + template_selection + skeleton_contract 기반만 허용.**

## 실행 전 확인

1. **Lock**: `work/.lock` == `UNLOCKED`. 아니면 중단.
2. **상태**: `current.phase` == `task_reviewed`. 아니면 "사용자 검토 미완료" 안내 후 중단.
3. **필수 입력**:
   - `work/task/2.Working/original/people_spec.md`
   - `work/task/2.Working/original/machine_spec.yml`
   - `work/task/2.Working/final/people_spec.md`
   - `work/task/2.Working/manifests/analyze.manifest.yml` (status: completed)
4. **금지**: `archive/**` 입력 금지. skeleton 미정의 artifact 직접 작성 금지.

## 필수 참조 파일 (읽기 전용)

- `system/policies/framework/template_selection.yml`
- `system/policies/framework/skeleton_contract.yml`
- `system/policies/framework/forbidden_apis.yml`
- `system/policies/framework/html_patterns.yml`, `xml_patterns.yml`, `controller_patterns.yml`, `service_patterns.yml`
- `system/templates/screen-types/*.yml`
- `system/templates/skeletons/**`
- `system/config/naming.yml`
- `system/schemas/machine_spec.schema.json`, `build_manifest.schema.json`, `verify_result.schema.json`

## 수행 절차

### 1. Workspace 잠금
`work/.lock` ← `LOCKED:task:build`, active_context 갱신

### 2. build.manifest 초기화
`work/task/2.Working/manifests/build.manifest.yml` (status: in_progress)

### 3. People Spec diff 분석
original vs final people_spec 차이 분석. 변경 목록 작성.

### 4. Machine Spec(final) 생성
- original/machine_spec.yml + people_spec diff → final/machine_spec.yml
- machine_spec.schema.json 검증
- **unresolved 존재 시 즉시 중단** — 사용자에게 미해결 항목 안내
- 저장: `work/task/2.Working/final/machine_spec.yml`

### 5. Generate Skill 호출
`generate` skill에 전달: final/machine_spec.yml, template_selection, skeleton_contract, screen-types, skeletons, framework policies.
반환: generated_paths[], self_check[], violations[]

### 6. Verify Result 생성
`work/task/2.Working/manifests/verify_result.yml` — checks: placeholder_complete, forbidden_pattern, naming_convention, audit_columns, comp_cd_filter

### 7. 위반 처리
errors → 보정 시도, 불가 시 failed. warnings → 경고 후 계속.

### 8. Deliverables/Report 정리
generated → `work/task/3.Result/deliverables/`, 보고서 → `work/task/3.Result/report/`

### 9. build.manifest 완료
status: completed, outputs, selected_templates, artifact_plan, self_check

### 10. Active Context 갱신
current.phase: task_built, task.status: built, lock ← UNLOCKED

### 11. 결과 보고
생성 파일 목록, verify 요약, deliverables 경로, `/fix` 또는 `/clean` 안내

## 실패/성공 조건
실패: 입력 누락, unresolved, skeleton 없음, verify error → failed, lock 해제
성공: final/machine_spec + generated + verify_result + deliverables + manifest (completed)
