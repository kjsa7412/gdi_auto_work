# /build command

## 목적
사용자 검토가 끝난 people_spec(final)과 machine_spec(original)을 기반으로 final/machine_spec.yml을 확정하고, skeleton 기반으로 코드 산출물을 생성한다.

**절대 규칙: skeleton 없는 자유 생성 금지. skeleton + template_selection + skeleton_contract 기반만 허용.**

## 실행 전 확인

1. **Lock**: `work/.lock` == `UNLOCKED`. 아니면 중단.
2. **상태**: `current.phase` == `task_analyzed`. 아니면 "먼저 /analyze를 실행하세요" 안내 후 중단.
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
```
work/.lock ← "LOCKED:task:build"
work/.active_context.yml ← current.command: "build", current.started_at: {now}
```

### 2. build.manifest 초기화
`work/task/2.Working/manifests/build.manifest.yml` (status: in_progress)

### 3. Review Skill 호출 → final/machine_spec.yml 생성
**build의 첫 번째 핵심 단계.** `review` skill을 호출한다.

review skill에 전달:
- `original/people_spec.md`
- `final/people_spec.md`
- `original/machine_spec.yml`
- `naming.yml`, `template_selection.yml`, `screen-types/*.yml`
- `machine_spec.schema.json`

review skill이 수행하는 것:
1. final people_spec frontmatter 검증
2. original vs final people_spec 차이 분석 (diff_summary)
3. diff를 반영하여 `final/machine_spec.yml` 생성
4. machine_spec schema 검증

review skill 반환:
- `review_result.status` — completed 또는 failed
- `review_result.diff_summary` — 변경 요약
- `review_result.machine_spec_final_path` — final/machine_spec.yml 경로
- `review_result.unresolved` — 미해결 항목

**review 실패 시 즉시 build 중단.** unresolved 존재 시 사용자에게 안내 후 중단.

### 4. Generate Skill 호출 → 코드 생성
review가 성공하면, **이후 모든 단계는 final/machine_spec.yml만을 기준으로 진행한다.**

`generate` skill에 전달:
- `work/task/2.Working/final/machine_spec.yml`
- `template_selection.yml`, `skeleton_contract.yml`
- `screen-types/*.yml`, `skeletons/**`
- `framework policies`

generate skill 반환:
- `generated_paths[]`
- `self_check[]`
- `violations[]`

### 5. Verify Result 생성
`work/task/2.Working/manifests/verify_result.yml`:
- checks: placeholder_complete, forbidden_pattern, naming_convention, audit_columns, comp_cd_filter

### 6. 위반 처리
errors → 보정 시도, 불가 시 failed. warnings → 경고 후 계속.

### 7. Deliverables/Report 정리
generated → `work/task/3.Result/deliverables/`
보고서 → `work/task/3.Result/report/build_report.md`

### 8. build.manifest 완료
```yaml
status: "completed"
inputs:
  people_spec_original: "work/task/2.Working/original/people_spec.md"
  people_spec_final: "work/task/2.Working/final/people_spec.md"
  machine_spec_original: "work/task/2.Working/original/machine_spec.yml"
outputs:
  machine_spec_final: "work/task/2.Working/final/machine_spec.yml"
  generated_paths: [...]
  deliverables_path: "work/task/3.Result/deliverables"
  report_path: "work/task/3.Result/report"
review_summary: {diff_summary from review skill}
selected_templates: [...]
artifact_plan: [...]
violations: []
self_check: [...]
```

### 9. Active Context 갱신
```
current.phase: "task_built"
task.status: "built"
task.last_build_at: {now}
task.machine_spec_final_path: "work/task/2.Working/final/machine_spec.yml"
work/.lock ← "UNLOCKED"
```

### 10. 결과 보고
- review diff 요약 (변경 사항)
- 생성 파일 목록
- verify 결과 요약
- deliverables 경로
- 문제 발견 시 `/fix`, 완료 시 `/clean` 안내

## 실패 조건
- 필수 입력 누락 → 즉시 중단
- review skill 실패 (unresolved, schema 불량) → 즉시 중단
- skeleton 파일 없음 → 즉시 중단
- verify_result에 error + 보정 불가 → failed
- 실패 시: phase → "failed", lock 해제

## 성공 조건
- `final/machine_spec.yml` 존재 + schema 통과
- generated artifacts 존재
- verify_result 존재 (status: pass 또는 warn)
- deliverables 배치 완료
- build.manifest status: completed
