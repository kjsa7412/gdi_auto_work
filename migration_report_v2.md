# v5 잔존 정책 이관 보고서 (2차)

**작업일**: 2026-04-07
**작업 근거**: 작업지시서12.md

---

## 1. 변경된 정책 파일 일람

### 신규 생성 — Framework 정책 (5개)
| 파일 | 설명 |
|------|------|
| `system/policies/framework/sql_generation.yml` | SQL 산출물 생성 조건, 파일명 규칙, 출력 위치, execution_order 연계 |
| `system/policies/framework/code_registration.yml` | 코드등록 SQL comm_cd 규칙, 기존 코드그룹 재사용 금지, 신규 코드 제안 기준 |
| `system/policies/framework/button_mapping.yml` | 표준/비표준 버튼 매핑, etc{N} listener, 자동 교정, 매핑 실패 처리 |
| `system/policies/framework/code_component_defaults.yml` | 코드콤보/코드헬프 기본값, DATA_VIEW/DATA_CODE_TYPE 상수, elements 직접 선언, 의존성 패턴 |
| `system/policies/framework/postgresql_rules.yml` | PostgreSQL 금지 패턴 (집계+윈도우, 중첩집계, STRING_AGG), 자동교정 가능 패턴 |

### 신규 생성 — Verify 정책 (7개)
| 파일 | 설명 |
|------|------|
| `system/policies/verify/html_checks.yml` | HTML 검증 18 critical + 3 warning (IIFE, PGM, fragment, 버튼, 코드콤보, 그리드 등) |
| `system/policies/verify/xml_checks.yml` | XML 검증 10 critical + 2 warning (namespace, resultType, audit, 금지컬럼 등) |
| `system/policies/verify/sql_checks.yml` | SQL 검증 6 critical (메뉴등록, pgm_path, 감사컬럼, execution_order) |
| `system/policies/verify/controller_checks.yml` | Controller 검증 6 critical (@Slf4j, 생성자주입, BaseResponse, CommonController 일관성) |
| `system/policies/verify/service_checks.yml` | Service 검증 6 critical (BaseService, reader/writer, @Transactional, namespace) |
| `system/policies/verify/self_diagnosis.yml` | 자기진단 4규칙 (DDL보완, 코드등록, 불명확항목, 자동/수동 구분) |
| `system/policies/verify/escalation_rules.yml` | 오류 escalation 4규칙 (자동수정, manual_required, warning, system-fix 트리거) + build 종료 정책 |

### 신규 생성 — Analyze 정책 (1개)
| 파일 | 설명 |
|------|------|
| `system/policies/analyze/default_resolution.yml` | 보완 우선순위 5단계 (DDL→코드정의서→화면유형→업무규칙→에이전트판단) + 태그 규칙 |

---

## 2. 변경된 command/skill/schema/config 파일 일람

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `.claude/skills/generate.md` | **재작성** | 정책 상세 제거, 외부 정책 참조 구조화, verify 정책 기반 검증 절차 명시, SQL 산출물 생성 단계 추가, 결과 구조에 verify_details/self_diagnosis/critical_pass/manual_required 추가 |
| `.claude/commands/build.md` | **보강** | verify 정책 파일 참조 추가, generate 반환에 verify_details/self_diagnosis/critical_pass 반영, verify_result 구조 확장, 위반 처리에 escalation_rules 기반 분기 추가, deploy 진입 조건에 critical_pass/manual_required 반영 |
| `.claude/commands/fix.md` | **보강** | verify 정책 기반 오류 판정 추가, 단순 patch vs 정책 patch 구분 명확화, system-fix 자동 호출 조건 추가, severity 기반 처리(high 자동/low 제안) 반영, 오류 분류 코드 체계 추가 |
| `.claude/skills/system-fix.md` | **보강** | severity 판정 기준 추가, backup 필수화, proposals/ 저장 위치 명시, high/low 처리 분기, 단발성/시스템적 fix 경계 표 추가, 판정 기준(정책/스키마/흐름/템플릿/런타임) 명시 |
| `system/schemas/verify_result.schema.json` | **확장** | critical_pass, manual_required, auto_fixed, details(html/xml/sql/controller/service_checks), self_diagnosis(ddl_supplements, code_registrations, unclear_warnings, auto_fixed, manual_required) 필드 추가, $defs/check_summary 정의 |
| `system/config/paths.yml` | **보강** | proposals, proposals_new, proposals_done, proposals_backup 경로 추가 |
| `system/policies/runtime/allowed_paths.yml` | **보강** | fix command에 system/policies/**, system/templates/**, proposals/policy_changes/** 쓰기 허용 추가, system_fix_writable 섹션 추가 |
| `readme.md` | **보강** | 정책 체계에 framework 5개, analyze 1개, verify 7개 추가, 디렉토리 구조에 proposals/ 추가, verify_result manifest 설명 확장, 정책 반영 흐름 다이어그램 추가 |

---

## 3. 변경 근거 요약

| 변경 | 근거 |
|------|------|
| framework 정책 5개 신설 | v5 generate.md 본문에 하드코딩된 SQL/코드/버튼/컴포넌트/PostgreSQL 규칙을 외부화 |
| verify 정책 7개 신설 | v5 generate.md Integrated Verify에 하드코딩된 검증 규칙을 artifact 유형별로 분리 |
| analyze 정책 1개 신설 | v5 analyze.md PHASE 4에 하드코딩된 보완 우선순위를 정책 파일로 승격 |
| generate.md 재작성 | 정책 상세를 외부 파일로 이동하고 참조 구조로 전환, verify 절차를 정책 기반으로 구조화 |
| build.md 보강 | verify_result의 새 필드(critical_pass, manual_required)를 build 성공/실패/deploy 진입 판정에 반영 |
| fix.md 보강 | verify 정책 기반 오류 분류, system-fix 자동 호출 조건, severity 기반 처리 분기 추가 |
| system-fix.md 보강 | severity 판정, backup 필수화, proposals/ 경로, 단발성/시스템적 fix 경계 명확화 |
| verify_result.schema.json 확장 | 새 정책 결과를 담을 수 있도록 details, self_diagnosis, critical_pass 등 필드 추가 |
| paths.yml/allowed_paths.yml 보강 | proposals/ 경로 추가, system-fix의 정책 파일 쓰기 권한 추가 |

---

## 4. 정책 반영 지점 요약

### generate skill (코드 생성 시)
- `sql_generation.yml` → 메뉴등록/DDL보완/코드등록 SQL 생성 판정
- `code_registration.yml` → 코드등록 SQL의 comm_cd 규칙 적용
- `button_mapping.yml` → 버튼 → listener 매핑, etc{N} 할당
- `code_component_defaults.yml` → 코드콤보/코드헬프 elements 생성
- `postgresql_rules.yml` → SQL 생성 시 금지 패턴 검증
- `html_checks.yml` ~ `service_checks.yml` → artifact별 자동 검증
- `self_diagnosis.yml` → DDL/코드 미등록 자동 탐지 → 보완 SQL 생성
- `escalation_rules.yml` → 자동 수정 / manual_required / system-fix 트리거

### build command (빌드 실행 시)
- `verify_result.critical_pass` → build 성공/실패 최종 판정
- `verify_result.manual_required` → 사용자 수동 확인 안내
- `verify_result.details` → artifact별 검증 결과 보고
- deploy 진입 조건에 `critical_pass=true, manual_required=false` 추가

### fix command (수정 실행 시)
- verify 정책의 check ID로 오류 매핑 → 정확한 원인 분류
- `escalation_rules.yml` ER-004 → system-fix 자동 호출 판정
- severity 기반 → high: 자동 적용, low: proposals/ 제안

### system-fix skill (시스템 보정 시)
- severity 판정 → backup → 정책 수정(high) 또는 proposals/new/ 기록(low)
- 적용 완료 시 proposals/done/ 으로 이동

---

## 5. 미반영/보류 항목

| 항목 | 사유 |
|------|------|
| `test.md` command | 작업지시서12 범위 밖 (Phase 2) |
| Playwright 테스트 | Phase 2 (실행 검증) |
| skeleton_contract.yml SQL 산출물 구조 추가 | SQL 산출물(메뉴등록 등)은 skeleton 치환이 아닌 정책 기반 생성이므로 skeleton_contract의 범위 밖. sql_generation.yml로 충분히 커버됨 |
| screen-types/*.yml 수정 | 새 정책(button_mapping, code_component 등)이 screen-type 정의 자체를 변경하지 않음. screen-type은 레이아웃/기능 정의이고, 새 정책은 생성/검증 규칙이므로 충돌 없음 |
| build_manifest.schema.json / fix_manifest.schema.json | build.md/fix.md에서 verify 섹션 구조를 명시했으므로 schema 업데이트는 선택적. 필요 시 후속 작업으로 보강 가능 |

---

## 완료 조건 충족 확인

| 조건 | 상태 |
|------|------|
| v5 잔존 정책이 정책/스키마/연계 규칙으로 정리됨 | ✅ 13개 정책 파일 생성 |
| 새 정책을 build/generate/fix/system-fix가 참조함 | ✅ 4개 command/skill 모두 수정 |
| verify 계층이 실질적으로 동작 가능한 구조 | ✅ 7개 verify 정책 + escalation + schema 반영 |
| verify_result 구조가 새 정책과 맞게 정리됨 | ✅ schema에 details/self_diagnosis/critical_pass 추가 |
| path/schema/command가 구버전 상태로 남지 않음 | ✅ paths.yml, allowed_paths.yml, schema, README 모두 수정 |
| 기존 1차 이관 결과와 충돌하지 않음 | ✅ 기존 파일 덮어쓰기 없음, 새 파일만 추가 |
