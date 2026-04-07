# 정책 중심 시스템 리팩토링 보고서

**작업일**: 2026-04-07
**작업 근거**: 작업지시서13.md

---

## 1. 리팩토링 전 상태 진단

### Command/Skill에 인라인으로 남아 있던 규칙성 자산

| 위치 | 인라인 규칙 | 문제 |
|------|-----------|------|
| `analyze.md` 37-55행 | DB 자격증명 하드코딩, psql 명령어 인라인 | db_access.yml에 이미 정의됨. 중복 |
| `analyze.md` 58-76행 | DB fallback 조건/절차 | db_access.yml에 이미 정의됨. 3곳 중복 |
| `analyze.md` 82-101행 | PPT→spec 생성 규칙, 미확정 처리 | 정책으로 분리 필요 |
| `build.md` 6행 | "절대 규칙: 자유 생성 금지" | 선언적 규칙이 command에 인라인 |
| `build.md` 84-106행 | Cache 검증/DB fallback | analyze와 동일 내용 중복 |
| `build.md` 169-174행 | Deploy 진입 조건 | deploy_policy.yml에 이미 정의됨. 중복 |
| `fix.md` 46-60행 | Issue type 분류 체계, patch 판정 표 | 규칙성 자산. 정책으로 승격 필요 |
| `fix.md` 62-82행 | DB fallback 절차 | 3곳 중복 |
| `fix.md` 96-106행 | 에러 코드 분류 체계 | 규칙성 자산. 정책으로 승격 필요 |
| `deploy.md` 38-42행 | Target path 계산 규칙 | deploy_policy.yml에 이미 정의됨. 중복 |
| `cache-refresh.md` 36-37행 | DB 자격증명 하드코딩 | db_access.yml에 이미 정의됨. 중복 |

### 규칙 중복 현황

| 규칙 | 중복 위치 | SSOT |
|------|----------|------|
| DB 자격증명/접속 방법 | analyze.md, cache-refresh.md, db_access.yml | db_access.yml |
| DB fallback 조건/절차 | analyze.md, build.md, fix.md, db_access.yml | db_access.yml |
| 진입 조건 (phase, lock, files) | 각 command.md, runtime.yml | runtime.yml |
| 필수 산출물 | 각 command.md, lifecycle.yml | lifecycle.yml |
| Deploy 진입 조건 | build.md, deploy_policy.yml | deploy_policy.yml |
| Target path 계산 | deploy.md, deploy_policy.yml | deploy_policy.yml |

---

## 2. 수행한 리팩토링

### 새 정책 파일 생성 (2개)

| 파일 | 승격 근거 |
|------|----------|
| `system/policies/verify/issue_classification.yml` | fix.md에 인라인된 issue type 분류 체계(5유형), patch 판정 기준 표, 에러 코드 분류 체계(9카테고리)를 정책으로 승격 |
| `system/policies/analyze/spec_generation.yml` | analyze.md에 인라인된 people_spec/machine_spec 생성 규칙, PPT 추출→분류 절차, 미확정 처리, cache 연계 규칙을 정책으로 승격 |

### Command 재작성 (5개)

| Command | Before | After |
|---------|--------|-------|
| `analyze.md` | 124줄. DB 자격증명 하드코딩, cache refresh 절차 인라인, DB fallback 상세 인라인, spec 생성 규칙 인라인 | 62줄. 정책 참조 테이블 + 절차는 정책 ID로만 지시 |
| `build.md` | 305줄. Cache 검증 절차 중복, deploy 진입 조건 중복, verify 구조 상세 인라인, manifest 구조 인라인 | 57줄. runtime.yml 기반 skill 호출 순서 + 정책 참조 |
| `fix.md` | 178줄. Issue 분류 체계 인라인, DB fallback 중복, 에러 코드 표 인라인 | 52줄. issue_classification.yml + escalation_rules.yml 참조 |
| `clean.md` | 51줄. 절차 상세 | 38줄. lifecycle.yml + runtime.yml 참조 |
| `cache-refresh.md` | 67줄. DB 자격증명 하드코딩 | 35줄. db_access.yml 참조 |

### Skill 재작성 (4개)

| Skill | Before | After |
|-------|--------|-------|
| `generate.md` | 162줄. Verify check ID 범위 열거 | 97줄. 정책 참조 + 절차만 |
| `review.md` | 83줄 | 54줄. 정책 참조 + 절차만 |
| `deploy.md` | 99줄. Target path 규칙 인라인 | 55줄. deploy_policy.yml 참조 |
| `system-fix.md` | 166줄. Severity 기준 인라인, 보완 대상 표 인라인 | 60줄. issue_classification.yml + escalation_rules.yml 참조 |

---

## 3. 리팩토링 후 책임 분리 구조

```
┌─────────────────────────────────────────────────────────┐
│  Policy Layer (SSOT)                                     │
│  system/policies/**/*.yml                                │
│  "무엇을 지켜야 하는가"                                      │
│                                                          │
│  ┌── analyze/ ───── ppt_extraction, classify_tags,       │
│  │                  screen_type_rules, default_resolution,│
│  │                  spec_generation                       │
│  ├── framework/ ─── layout_rules, html/xml/ctrl/svc      │
│  │                  patterns, template_selection,         │
│  │                  skeleton_contract, forbidden_apis,    │
│  │                  sql_generation, code_registration,    │
│  │                  button_mapping, code_component,       │
│  │                  postgresql_rules                      │
│  ├── verify/ ────── html/xml/sql/ctrl/svc_checks,        │
│  │                  self_diagnosis, escalation_rules,     │
│  │                  issue_classification                  │
│  └── runtime/ ───── allowed_paths, db_access,            │
│                     context_isolation, lifecycle,         │
│                     deploy_policy                         │
├─────────────────────────────────────────────────────────┤
│  Config Layer                                            │
│  system/config/*.yml + system/schemas/*.json              │
│  runtime.yml (state machine + command contracts)          │
│  paths.yml, naming.yml, framework_manifest.yml            │
├─────────────────────────────────────────────────────────┤
│  Command Layer (Thin Orchestrator)                        │
│  .claude/commands/*.md                                    │
│  "언제 어떤 순서로 실행하는가"                                  │
│  - 진입 조건 → runtime.yml 참조                              │
│  - 정책 로드 목록 → 테이블로 명시                               │
│  - 절차 → 정책 ID/파일로만 지시                                │
│  - 결과 보고 → 무엇을 보고할지만 기술                            │
├─────────────────────────────────────────────────────────┤
│  Skill Layer (Executor)                                   │
│  .claude/skills/*.md                                      │
│  "어떻게 실행하는가"                                          │
│  - 입력/출력 구조                                            │
│  - 정책 로드 → 적용 → 결과 반환                                │
│  - 규칙 상세는 포함하지 않음                                    │
├─────────────────────────────────────────────────────────┤
│  Manifest Layer (Evidence)                                │
│  work/**/manifests/*.yml                                  │
│  "어떤 policy를 어떤 근거로 준수했는가"                          │
│  verify_result → check ID별 pass/fail + policy 참조         │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 중복 제거 결과

| 규칙 | 리팩토링 전 중복 수 | 리팩토링 후 | SSOT |
|------|----------------|----------|------|
| DB 자격증명 | 3곳 (analyze, cache-refresh, db_access) | 1곳 | db_access.yml |
| DB fallback 절차 | 4곳 (analyze, build, fix, db_access) | 1곳 | db_access.yml |
| 진입 조건 | 6곳 (각 command + runtime.yml) | 1곳 | runtime.yml |
| 필수 산출물 | 6곳 (각 command + lifecycle.yml) | 1곳 | lifecycle.yml |
| Deploy 진입 조건 | 2곳 (build, deploy_policy) | 1곳 | deploy_policy.yml |
| Target path 규칙 | 2곳 (deploy, deploy_policy) | 1곳 | deploy_policy.yml |
| Issue 분류 체계 | 1곳 (fix.md 인라인) | 1곳 (정책) | issue_classification.yml |
| 에러 코드 체계 | 1곳 (fix.md 인라인) | 1곳 (정책) | issue_classification.yml |
| Spec 생성 규칙 | 1곳 (analyze.md 인라인) | 1곳 (정책) | spec_generation.yml |

---

## 5. 변경 파일 일람

### 신규
- `system/policies/verify/issue_classification.yml`
- `system/policies/analyze/spec_generation.yml`

### 재작성
- `.claude/commands/analyze.md` (124줄 → 62줄, -50%)
- `.claude/commands/build.md` (305줄 → 57줄, -81%)
- `.claude/commands/fix.md` (178줄 → 52줄, -71%)
- `.claude/commands/clean.md` (51줄 → 38줄, -25%)
- `.claude/commands/cache-refresh.md` (67줄 → 35줄, -48%)
- `.claude/skills/generate.md` (162줄 → 97줄, -40%)
- `.claude/skills/review.md` (83줄 → 54줄, -35%)
- `.claude/skills/deploy.md` (99줄 → 55줄, -44%)
- `.claude/skills/system-fix.md` (166줄 → 60줄, -64%)

### 보강
- `readme.md` — 책임 분리 구조 섹션, 정책 테이블 업데이트

---

## 6. 기대 상태 충족 확인

| 기준 | 충족 |
|------|------|
| command를 읽으면 절차와 phase 흐름이 보인다 | ✅ 진입→정책로드→절차(정책참조)→보고 구조 |
| skill을 읽으면 실행 책임과 입출력이 보인다 | ✅ 입력→정책로드→절차→반환 구조 |
| policy를 읽으면 시스템 규칙 실체가 보인다 | ✅ id, severity, rule, action 정형 구조 |
| 같은 규칙이 여러 곳에 중복 서술 안 됨 | ✅ DB fallback 4→1, 진입조건 6→1 등 |
| 새 규칙 추가 시 policy 추가가 우선 | ✅ command/skill은 정책 참조만 |
| 정책 폴더만 보고 시스템 규칙 파악 가능 | ✅ 4도메인 26파일로 전체 규칙 커버 |

---

## 7. 정책 파일 전체 인벤토리 (26개)

| 도메인 | 파일 수 | 파일 목록 |
|--------|--------|----------|
| analyze (5) | 5 | ppt_extraction, classify_tags, screen_type_rules, default_resolution, spec_generation |
| framework (13) | 13 | layout_rules, html_patterns, xml_patterns, controller_patterns, service_patterns, template_selection, skeleton_contract, forbidden_apis, sql_generation, code_registration, button_mapping, code_component_defaults, postgresql_rules |
| verify (8) | 8 | html_checks, xml_checks, sql_checks, controller_checks, service_checks, self_diagnosis, escalation_rules, issue_classification |
| runtime (5) | 5 | allowed_paths, db_access, context_isolation, lifecycle, deploy_policy |
