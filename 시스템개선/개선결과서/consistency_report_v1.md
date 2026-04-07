# 시스템 정합성 리팩토링 보고서

**작업일**: 2026-04-07
**작업 근거**: 작업지시서14.md

---

## 1. 감사 방법

전체 레포를 대상으로 5개 관점에서 교차 검증 수행:
- **구조 정합성**: README 디렉토리 트리 ↔ 실제 파일시스템
- **실행 정합성**: command/skill이 전제하는 파일 ↔ 실제 존재 여부
- **정책 정합성**: policy 참조 ↔ policy 파일 존재/구조
- **기록 정합성**: manifest schema ↔ command/skill이 기록하는 구조
- **운영 정합성**: runtime/paths/allowed_paths ↔ 실제 디렉토리

---

## 2. 발견된 불일치 및 수정

### ISSUE-1: fix_manifest.schema.json — issue_type enum 불일치 (CRITICAL)

**발견**: schema의 `issue_type` enum이 `["output_patch", "skeleton_patch", "reanalyze", "rebuild"]`로 4개만 정의.
fix.md와 `issue_classification.yml`은 `"policy_patch"`를 5번째 유형으로 정의.

**영향**: policy_patch 유형의 fix가 schema 검증 실패.

**수정**: `fix_manifest.schema.json` → issue_type enum에 `"policy_patch"` 추가.

---

### ISSUE-2: fix_manifest.schema.json — 신규 필드 누락 (CRITICAL)

**발견**: fix.md가 기록하는 `error_classification`, `system_fix`, `verify` 섹션이 schema에 미정의.

**영향**: fix.manifest에 이 필드들을 기록하면 `additionalProperties: false` 규칙으로 schema 검증 실패.

**수정**: 3개 섹션 추가:
- `error_classification`: { category, check_id, severity }
- `system_fix`: { invoked, severity, proposals[] }
- `verify`: { critical_pass, manual_required, auto_fixed[] }

---

### ISSUE-3: build_manifest.schema.json — verify 구조 불일치 (CRITICAL)

**발견**: build.md가 기록하는 `review_summary`, `verify` (critical_pass, details, self_diagnosis) 섹션이 schema에 미정의. 구 `self_check` 필드만 존재.

**영향**: build.manifest에 새 verify 구조를 기록하면 schema 검증 실패.

**수정**: 
- `review_summary` 섹션 추가
- `verify` 섹션 추가 (critical_pass, auto_fixed, manual_required, details, self_diagnosis)
- 구 `self_check` 제거, `violations`은 하위호환을 위해 유지

---

### ISSUE-4: runtime.yml — cache-refresh command contract 누락 (MODERATE)

**발견**: runtime.yml의 `commands` 섹션에 analyze, build, fix, clean 4개만 정의. cache-refresh command contract 없음. `cache.refresh_commands`에 "cache-refresh"가 언급되지만 독립 contract 없음.

**영향**: cache-refresh command의 진입 조건, 입출력, 상태 전이가 SSOT에 미등록.

**수정**: `commands.cache-refresh` 섹션 추가:
- start_condition: 모든 phase에서 실행 가능
- inputs: `system/cache/convention/sql/*.sql`
- outputs: `system/cache/convention/*.txt` (5개)
- success_state: null (phase 변경 없음)

---

### ISSUE-5: README — system/runtime/ 디렉토리 미표시 (MINOR)

**발견**: 실제로 `system/runtime/active/task/`, `system/runtime/active/fix/`, `system/runtime/logs/` 디렉토리 존재하고 `paths.yml`에도 정의됨. 그러나 README 디렉토리 트리에 미표시.

**수정**: README 디렉토리 트리에 `system/runtime/` 섹션 추가.

---

### ISSUE-6: README — 프로젝트 이력 미갱신 (MINOR)

**발견**: `migration_report_v2.md`, `refactoring_report_v1.md`가 존재하지만 README 이력 테이블에 미등록.

**수정**: 이력 테이블에 2개 항목 추가.

---

### ISSUE-7: README — Manifest 체계 설명 불일치 (MINOR)

**발견**: build.manifest, fix.manifest의 주요 필드 설명이 schema 확장 전 구버전 기준.

**수정**: Manifest 체계 테이블 업데이트 (verify, system_fix 등 반영).

---

## 3. 정합성 확인 결과 (수정 후)

### 구조 정합성
| 항목 | 결과 |
|------|------|
| README 디렉토리 트리 ↔ 실제 파일시스템 | ✅ 일치 |
| paths.yml 경로 ↔ 실제 디렉토리 | ✅ 일치 (89줄, 모든 경로 존재) |
| system/runtime/ 설명 ↔ 실디렉토리 | ✅ 일치 (README에 추가) |
| proposals/ 설명 ↔ 실디렉토리 | ✅ 일치 (new/done/backup 존재) |

### 실행 정합성
| 항목 | 결과 |
|------|------|
| command 참조 파일 ↔ 실제 존재 | ✅ 모든 참조 파일 존재 (31 정책 + 9 스키마 + 16 스켈레톤) |
| skill 입력 ↔ command 출력 | ✅ 일치 (review→generate→deploy 체인) |
| runtime.yml command contracts ↔ 실제 command | ✅ 일치 (5개 모두 등록) |
| state machine 전이 ↔ command 진입 조건 | ✅ 일치 |

### 정책 정합성
| 항목 | 결과 |
|------|------|
| command 정책 참조 ↔ policy 파일 존재 | ✅ 31/31 존재 |
| verify check ID ↔ verify 정책 파일 | ✅ HC/XC/SC/CC/SVC/SD/ER/PG 전체 매핑 |
| issue_classification ↔ fix schema | ✅ 5유형 일치 (policy_patch 추가) |
| escalation_rules ↔ build/fix 종료 조건 | ✅ critical_pass 기반 일치 |

### 기록 정합성
| 항목 | 결과 |
|------|------|
| build_manifest.schema ↔ build.md 기록 구조 | ✅ 일치 (review_summary, verify 추가) |
| fix_manifest.schema ↔ fix.md 기록 구조 | ✅ 일치 (error_classification, system_fix, verify 추가) |
| verify_result.schema ↔ generate.md 반환 구조 | ✅ 일치 (details, self_diagnosis, critical_pass) |
| analyze_manifest.schema ↔ analyze.md 기록 구조 | ✅ 일치 |

### 운영 정합성
| 항목 | 결과 |
|------|------|
| allowed_paths ↔ 실제 경로 | ✅ 일치 |
| context_isolation ↔ command lock 형식 | ✅ 일치 |
| lifecycle mandatory_outputs ↔ schema required | ✅ 일치 |
| db_access cache 경로 ↔ paths.yml | ✅ 일치 |

---

## 4. SSOT 맵 (진실원천 고정)

| 관심사 | SSOT | 소비자 |
|--------|------|--------|
| 시스템 규칙 | `system/policies/**/*.yml` (31개) | command, skill |
| 실행 계약 | `system/config/runtime.yml` | command (진입 조건) |
| 경로 체계 | `system/config/paths.yml` | 전체 시스템 |
| 네이밍 규칙 | `system/config/naming.yml` | analyze, review, generate |
| 산출물 구조 | `system/schemas/*.json` (9개) | manifest 생성/검증 |
| 시스템 개요 | `readme.md` | 사용자/유지보수자 |
| Phase 절차 | `.claude/commands/*.md` (5개) | Claude Code |
| 실행 로직 | `.claude/skills/*.md` (4개) | command |
| 생성 골격 | `system/templates/skeletons/**` (16개) | generate skill |
| 화면유형 | `system/templates/screen-types/*.yml` (4개) | analyze, review, generate |

---

## 5. 변경 파일 일람

| 파일 | 변경 | 근거 |
|------|------|------|
| `system/schemas/fix_manifest.schema.json` | issue_type에 policy_patch 추가, error_classification/system_fix/verify 섹션 추가 | ISSUE-1, ISSUE-2 |
| `system/schemas/build_manifest.schema.json` | review_summary 추가, verify 섹션 추가, 구 self_check 제거 | ISSUE-3 |
| `system/config/runtime.yml` | cache-refresh command contract 추가 | ISSUE-4 |
| `readme.md` | runtime/ 디렉토리 표시, 이력 추가, manifest 설명 갱신 | ISSUE-5, 6, 7 |

---

## 6. 향후 깨지지 않는 구조를 위한 원칙

### 변경 시 동기화 체크리스트

새 정책 추가 시:
1. `system/policies/{domain}/` 에 yml 생성
2. 해당 command/skill의 "필수 정책 로드" 테이블에 추가
3. README 정책 체계 테이블에 추가

새 command 추가 시:
1. `.claude/commands/` 에 md 생성
2. `runtime.yml` → `commands` 섹션에 contract 추가
3. `allowed_paths.yml`에 경로 규칙 추가
4. `.claude/settings.json` → `workflow.commands`에 등록
5. README에 반영

새 manifest 필드 추가 시:
1. 해당 `system/schemas/*.schema.json` 수정
2. command/skill의 기록 구조 확인
3. README manifest 체계 테이블 확인

### 검증 방법
- **구조 검증**: paths.yml의 모든 경로가 실제 존재하는지 확인
- **참조 검증**: command/skill이 참조하는 모든 파일이 존재하는지 확인
- **schema 검증**: manifest를 schema로 validate
- **SSOT 검증**: 동일 규칙이 2곳 이상에 정의되어 있지 않은지 확인
