# system-fix skill

## 역할
반복/구조적 오류를 시스템 차원에서 분석하고, 정책/템플릿/skeleton 보완을 수행하는 **실행자**.
일회성 데이터 오류가 아닌 시스템 전체에 반영해야 하는 수정을 담당.

## 입력
- fix 요청 (`work/fix/1.Prep/*`)
- `work/fix/2.Working/manifests/fix.manifest.yml`
- `work/task/2.Working/manifests/verify_result.yml`

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/policies/verify/issue_classification.yml` | 오류 유형 분류, 에러 코드 체계 |
| `system/policies/verify/escalation_rules.yml` | severity 기반 처리, system-fix 트리거 |
| `system/policies/framework/*.yml` | 수정 대상 정책 |
| `system/policies/verify/*.yml` | 수정 대상 검증 정책 |
| `system/templates/**` | 수정 대상 템플릿/skeleton |
| `system/schemas/**` | 수정 대상 schema |
| `system/config/paths.yml` | proposals 경로 |

## 수행 절차

### 1. 오류 원인 분류
`issue_classification.yml` 기준으로:
- 일회성 데이터 오류 → system-fix 불필요, 반환
- 반복 구조 오류 → 계속 진행

### 2. Severity 판정
`escalation_rules.yml` 기준:
- **high**: 금지 API 누락, critical 검증 규칙 gap, 반복 발생 → 자동 적용
- **low**: CSS, warning 수준, 선호도 → 제안만

### 3. 보완 대상 식별
오류 원인에서 수정이 필요한 시스템 파일 식별:
- 정책 → `system/policies/**/*.yml`
- 구조 검증 → `system/schemas/**/*.json`
- 템플릿 → `system/templates/skeletons/**`
- 설정 → `system/config/*.yml`

### 4. Backup (정책 변경 전 필수)
`proposals/policy_changes/backup/{filename}.{timestamp}.bak`
backup 실패 → 정책 변경 중단.

### 5. Patch 생성 및 적용
- **high severity**: backup 후 정책 파일에 즉시 적용 → `proposals/policy_changes/done/`
- **low severity**: `proposals/policy_changes/new/` 에 제안만 기록

### 6. 결과 반환
```yaml
issue_classification: "..."
severity: "high" | "low"
recommended_patch_targets: [...]
auto_fix_possible: true|false
patch_proposals: [{ target_file, change_type, severity, applied, backup_path }]
proposals_saved:
  new: []
  done: []
  backups: []
```

## 판정 원칙
- **근거 기반만** 제안. 추정으로 정책 변경 금지.
- 실제 반복 패턴 확인 시에만 system 파일 수정 제안.
