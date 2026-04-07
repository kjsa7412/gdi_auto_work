# deploy skill

## 역할
build 또는 fix 결과물을 프로젝트 target path에 **자동 배포**하는 실행자.

## 입력
- deliverables 경로 (`work/task/3.Result/deliverables/**` 또는 `work/fix/3.Result/deliverables/**`)
- mode: **"apply"** (기본) 또는 "prepare-only"

## 호출 시점
- **build 완료 직후**: verify pass/warn이고 deliverables 존재 시 자동 호출
- **fix 완료 직후**: verify pass/warn이고 fix deliverables 존재 시 자동 호출
- 사용자 확인 없이 즉시 실행

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/policies/runtime/deploy_policy.yml` | 모드, 진입 조건, target path 규칙, conflict 처리, 실패 영향 |
| `system/config/framework_manifest.yml` | source_roots, supported_artifacts |
| `system/policies/runtime/context_isolation.yml` | deploy 격리 규칙 |

## 수행 절차

### 1. 대상 확인
deliverables에서 배포 대상 파일 수집. 파일 유형별 분류:
- **파일 배포 대상**: HTML, XML, Controller(.java), Service(.java)
- **DB 실행 대상**: SQL 파일 → 파일 복사하지 않음, deploy_summary에 "DB 직접 실행 필요"로 기록

### 2. Target Path 계산
`deploy_policy.yml` → `target_path_resolution.rules` + `framework_manifest.yml` → `source_roots` 적용:
- HTML → `{templates_root}/project/{module_group}/{sub_group}/{MODULE_ID}.html`
- XML → `{mappers_root}/sjerp/{module_group}/{sub_group}/{module_id}.xml`
- Controller → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/{ModuleId}Controller.java`
- Service → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/{ModuleId}Service.java`

계산 불가 → status: "path_unknown"

### 3. Conflict 검토 + 자동 덮어쓰기
기존 파일이 target path에 존재하면:
- conflict 사실을 deploy_summary에 기록
- **자동으로 덮어쓰기** (사용자 확인 불필요)

### 4. 파일 복사 실행
target path에 디렉토리가 없으면 생성 후 파일 복사.

### 5. Deploy Summary 생성
```yaml
deploy_summary:
  mode: "apply"
  status: "deployed"
  total_deployed: N
  total_pending: M    # SQL 등 수동 실행 대상
  deployed_files: [{ source, target, status, conflict }]
  pending_manual: [{ source, target, note }]
  conflicts: []
```

출력 위치:
- build에서 호출: `work/task/3.Result/report/deploy_summary.yml`
- fix에서 호출: `work/fix/3.Result/report/deploy_summary.yml`

## 실패 조건
deliverables 없음, target path 전체 계산 불가, framework_manifest 누락.
**deploy 실패는 build/fix 실패가 아님** — manifest에 deploy.status만 변경.
