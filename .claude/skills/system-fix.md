# system-fix skill

## 목적
반복 오류 또는 구조적 오류를 시스템 차원에서 분석하고, framework policy / template / skeleton / runtime rule 보완 제안을 생성한다. 일회성 데이터 오류가 아닌 **시스템 전체에 반영되어야 하는 수정사항**을 담당한다.

## 입력

- fix 요청 파일 (`work/fix/1.Prep/*`)
- `work/fix/2.Working/manifests/fix.manifest.yml`
- build 또는 analyze verify_result (`work/task/2.Working/manifests/verify_result.yml`)
- 관련 policy: `system/policies/framework/**`
- 관련 template: `system/templates/**`
- 관련 skeleton: `system/templates/skeletons/**`
- 관련 schema: `system/schemas/**`

## 수행 절차

### 1. 오류 원인 분류
fix.manifest의 issue_type과 fix 요청 내용을 분석하여 원인을 분류:
- **일회성 데이터 오류**: 특정 화면의 컬럼/필드 잘못 → system-fix 불필요
- **반복 구조 오류**: 동일 유형 오류가 다른 화면에서도 발생 가능 → system-fix 필요
  - skeleton 구조 결함
  - placeholder 계약 부족
  - framework policy 누락
  - template_selection 규칙 부정확
  - naming 규칙 불완전
  - **convention cache 불일치**: cache의 코드/테이블/함수 정보와 실제 산출물이 맞지 않음 → cache refresh 또는 SQL 보정 필요

### 2. 보완 대상 식별
오류 원인에 따라 수정이 필요한 시스템 파일을 식별:

| 원인 | 보완 대상 |
|------|-----------|
| skeleton 구조 결함 | `system/templates/skeletons/**` |
| placeholder 계약 부족 | `system/policies/framework/skeleton_contract.yml` |
| framework policy 누락 | `system/policies/framework/*.yml` |
| template_selection 부정확 | `system/policies/framework/template_selection.yml` |
| screen-type 정의 부족 | `system/templates/screen-types/*.yml` |
| naming 규칙 불완전 | `system/config/naming.yml` |
| runtime 계약 누락 | `system/config/runtime.yml`, `system/policies/runtime/*.yml` |
| cache-산출물 불일치 | `system/cache/convention/*.txt` 갱신 또는 `system/cache/convention/sql/*.sql` 보정 |

### 3. 자동 패치 가능/불가 판단
- **자동 패치 가능**: 명확한 규칙 추가, 패턴 보완 등 → patch proposal 생성
- **자동 패치 불가**: 설계 결정 필요, 다수 파일 영향 → 사용자 판단 요청

### 4. Patch Proposal 생성
자동 패치 가능한 경우:
```yaml
patch_proposal:
  target_file: "system/policies/framework/skeleton_contract.yml"
  change_type: "add_placeholder"
  description: "새로운 필수 placeholder 추가"
  before: "..."
  after: "..."
  auto_applicable: true
```

### 5. 결과 반환
fix command에 반환:
```yaml
issue_classification: "반복 구조 오류 - skeleton 구조 결함"
recommended_patch_targets:
  - "system/templates/skeletons/html/list.html"
  - "system/policies/framework/skeleton_contract.yml"
auto_fix_possible: true
patch_proposals: [...]
follow_up_actions:
  - "skeleton 보정 후 /build 재실행 필요"
```

fix.manifest에 반영할 정보:
- `impact_scope.policy_patch_required: true`
- `actions_taken`에 system-fix 결과 기록

## 실패 조건
- 원인 분류 불가
- 관련 근거 파일 부족

## 중요
system-fix는 **근거 기반**으로만 제안한다. 추정으로 정책을 변경하지 않는다. 실제 반복 패턴이 확인된 경우에만 system 파일 수정을 제안한다.
