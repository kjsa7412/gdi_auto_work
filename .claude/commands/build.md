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
- `system/cache/convention/*.txt` — convention cache (cache-first 참조)

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

### 4. Convention Cache 참조 검증 및 DB Fallback
final/machine_spec.yml의 테이블명, 컬럼명, 코드 정의, SQL ID가 convention cache와 일치하는지 검증한다.

**검증 항목**:
- `main_table`이 `system/cache/convention/table.txt`에 존재하는지
- `detail_table`이 존재하는지 (list-detail인 경우)
- 코드 타입이 `system/cache/convention/code.txt`에 정의되어 있는지
- 함수 참조(fn_*)가 `system/cache/convention/function.txt`에 존재하는지
- mapper namespace / sql id 규칙이 naming.yml과 일치하는지

**cache hit**: 필요한 정보가 cache에서 모두 확인됨 → 검증 통과
**cache miss/insufficient**: 필요한 정보가 cache에 없거나 부족함 → DB fallback 판정

**DB fallback 절차** (build는 cache refresh를 하지 않지만, 검증에 필요한 만큼 DB 조회는 허용):
1. 부족한 카테고리 식별 (code/table/view/function)
2. `db_access.yml` connection 정보로 필요한 항목만 SELECT 조회
3. 조회 결과로 검증 수행
4. 필요 시 cache *.txt에 결과 보강 반영 (warning 기록)
5. fallback reason을 build.manifest에 기록

**DB fallback도 실패 시**: warning 기록 후 해당 검증 skip (cache_insufficient: true)

**결과 기록**: build.manifest에 cache.used, cache.hit_categories, cache.miss_categories, cache.insufficient, cache.mismatch_detected, cache.db_fallback_used, cache.db_fallback_reason[], cache.db_fallback_status 기록

### 5. Generate Skill 호출 → 코드 생성
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

### 6. Verify Result 생성
`work/task/2.Working/manifests/verify_result.yml`:
- checks: placeholder_complete, forbidden_pattern, naming_convention, audit_columns, comp_cd_filter

### 7. 위반 처리
errors → 보정 시도, 불가 시 failed. warnings → 경고 후 계속.

### 8. Deliverables/Report 정리
generated → `work/task/3.Result/deliverables/`
보고서 → `work/task/3.Result/report/build_report.md`

### 9. Deploy 진입 판정 및 선택적 Deploy 준비

**build의 핵심 성공 조건은 여기까지(생성/검증/deliverables 정리)로 이미 충족된다.**
deploy는 build의 필수 성공 조건이 아니며, 선택적 후속 단계이다.

#### 9-1. Deploy 진입 조건 확인
아래 조건이 **모두** 충족될 때만 deploy 준비 단계로 진입한다:
- `verify_result.status` == `pass` 또는 `warn`
- `work/task/3.Result/deliverables/` 경로에 파일이 1개 이상 존재
- deploy 금지 상태가 아님 (active_context에 deploy_skip 플래그 없음)

**진입 조건 미충족 시**: deploy.attempted = false로 기록하고 다음 단계(build.manifest 완료)로 진행.

#### 9-2. Deploy Skill 호출 (prepare-only)
`deploy` skill을 **prepare-only** 모드로 호출한다.

deploy skill에 전달:
- `work/task/3.Result/deliverables/**`
- `system/config/framework_manifest.yml` (source_roots)
- `mode: "prepare-only"`

deploy skill이 수행하는 것:
1. deliverables 존재 검증
2. framework_manifest의 source_roots 기반으로 파일별 target path 계산
3. overwrite/conflict 탐지
4. deploy_summary 생성
5. **실제 복사는 수행하지 않음**

deploy skill 반환:
- `deploy_summary.status` — prepared, warn, failed
- `deploy_summary.total_files` — 대상 파일 수
- `deploy_summary.target_paths[]` — source/target/status 매핑
- `deploy_summary.warnings[]` — 경고 목록
- `deploy_summary.conflicts[]` — 충돌 목록
- `deploy_summary.mode` — "prepare-only"

#### 9-3. Deploy 결과 기록
deploy 수행 결과를 build.manifest의 `deploy` 섹션에 기록한다:
```yaml
deploy:
  attempted: true
  mode: "prepare-only"
  status: "prepared"  # prepared | warn | failed | skipped
  summary_path: "work/task/3.Result/report/deploy_summary.yml"
  total_files: 2
  target_paths:
    - source: "deliverables/DTA030.html"
      target: "src/main/resources/templates/project/dt/dta/DTA030.html"
      status: "ready"
    - source: "deliverables/dta030.xml"
      target: "src/main/resources/mapper/sjerp/dt/dta/dta030.xml"
      status: "ready"
  warnings: []
  conflicts: []
```

#### 9-4. Deploy 실패 처리
- deploy prepare-only 단계 실패는 **build 결과물 자체를 무효화하지 않는다.**
- deploy.status를 `failed` 또는 `warn`으로 기록하고 build 진행을 계속한다.
- build.manifest의 status는 여전히 `completed`로 유지 (생성/검증이 성공했으므로).

### 10. build.manifest 완료
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
deploy:
  attempted: true
  mode: "prepare-only"
  status: "prepared"
  summary_path: "work/task/3.Result/report/deploy_summary.yml"
  total_files: N
  target_paths: [...]
  warnings: []
  conflicts: []
```

### 11. Active Context 갱신
```
current.phase: "task_built"
task.status: "built"
task.last_build_at: {now}
task.machine_spec_final_path: "work/task/2.Working/final/machine_spec.yml"
task.deploy_prepared: true | false
work/.lock ← "UNLOCKED"
```

### 12. 결과 보고
- review diff 요약 (변경 사항)
- 생성 파일 목록
- verify 결과 요약
- deliverables 경로
- **deploy 준비 결과**:
  - deploy 준비 완료 시: 대상 파일 수, target path 목록 안내
  - target path 미확정 시: "배포 준비까지만 완료" 안내
  - 실제 복사 미수행 시: "실제 파일 복사는 아직 수행되지 않았습니다. 필요 시 deploy apply를 별도 요청하세요." 표시
  - deploy 미진입 시: "verify 결과에 따라 deploy 준비를 건너뛰었습니다." 안내
- 문제 발견 시 `/fix`, 완료 시 `/clean` 안내

## 실패 조건
- 필수 입력 누락 → 즉시 중단
- review skill 실패 (unresolved, schema 불량) → 즉시 중단
- skeleton 파일 없음 → 즉시 중단
- verify_result에 error + 보정 불가 → failed
- 실패 시: phase → "failed", lock 해제
- **deploy prepare-only 실패는 build 실패가 아님** (deploy.status만 failed/warn으로 기록)

## 성공 조건
- `final/machine_spec.yml` 존재 + schema 통과
- generated artifacts 존재
- verify_result 존재 (status: pass 또는 warn)
- deliverables 배치 완료
- build.manifest status: completed
- **deploy는 선택적이므로 deploy 실패와 무관하게 build 성공 가능**
