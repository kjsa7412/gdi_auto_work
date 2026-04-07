# deploy skill

## 역할
build 또는 fix 결과물의 배포 준비/반영을 수행하는 **실행자**.

## 입력
- deliverables 경로 (`work/task/3.Result/deliverables/**` 또는 `work/fix/3.Result/deliverables/**`)
- mode: "prepare-only" (기본) 또는 "apply"

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/policies/runtime/deploy_policy.yml` | 모드, 진입 조건, target path 규칙, conflict 처리, 실패 영향 |
| `system/config/framework_manifest.yml` | source_roots, supported_artifacts |
| `system/policies/runtime/context_isolation.yml` | deploy 격리 규칙 |

## 수행 절차

### 1. 대상 확인
deliverables에서 배포 대상 파일 수집. 누락 파일 경고.

### 2. Target Path 계산
`deploy_policy.yml` → `target_path_resolution.rules` 적용:
- HTML → `{templates_root}/project/{module_group}/{sub_group}/{MODULE_ID}.html`
- XML → `{mappers_root}/sjerp/{module_group}/{sub_group}/{module_id}.xml`
- Controller → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/{ModuleId}Controller.java`
- Service → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/{ModuleId}Service.java`

계산 불가 → status: "path_unknown"

### 3. Conflict 검토
`deploy_policy.yml` → `conflict_handling` 적용.

### 4. Deploy Summary 생성
```yaml
deploy_summary:
  mode: "prepare-only" | "apply"
  status: "prepared" | "warn" | "failed" | "applied"
  total_files: N
  target_paths: [{ source, target, status }]
  warnings: []
  conflicts: []
  copied_paths: []  # apply 모드만
```

### 5. Apply 모드 (사용자 확인 후)
`deploy_policy.yml` → `modes.apply.requires_user_confirmation`: true.
conflict 없거나 사용자 승인 시 파일 복사.

## 실패 조건
deliverables 없음, target path 전체 계산 불가, framework_manifest 누락.
