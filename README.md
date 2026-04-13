# GDI Auto Work

GDI FERP Backoffice 화면 자동 생성 시스템.
PPT 화면설계서를 입력으로 받아, 프레임워크 규칙에 따라 HTML/XML/Controller/Service 코드를 자동 생성한다.

---

## 시스템 구조 요약

```
Policy (SSOT)          → system/policies/**/*.yml  — 무엇을 지켜야 하는가 (31개)
Config (Contract)      → system/config/*.yml       — 경로, 네이밍, 상태 전이, 프레임워크 정보
Schema (Validation)    → system/schemas/*.json     — manifest/spec 구조 검증 (9개)
Template (Generation)  → system/templates/**       — skeleton, 화면유형, spec 템플릿
Command (Orchestrator) → .claude/commands/*.md     — phase 전환, skill 호출 순서 (5개)
Skill (Executor)       → .claude/skills/*.md       — 정책 로드 → 실행 → 결과 반환 (4개)
Manifest (Evidence)    → work/**/manifests/*.yml   — 실행 결과, 판정 근거, 검증 증적
```

---

## 핵심 원칙

| 원칙 | 설명 |
|------|------|
| Skeleton 기반 생성 | 자유 코드 생성 금지. skeleton + placeholder 치환만 허용 |
| Dual-Spec | people_spec.md (사람 검토) + machine_spec.yml (기계 입력) 분리 |
| Single-Active Workspace | 동시에 하나의 task 또는 fix만 실행 |
| Cache-First | convention cache 우선 참조 → 부족 시 DB fallback (SELECT only) |
| Manifest 기반 입출력 | 모든 command는 manifest로 입출력/판정 근거를 기록 |
| Archive = History Only | archive/는 보관소. 실행 입력으로 사용 금지 |
| Policy-First | 규칙은 정책 파일이 SSOT. command/skill은 정책 소비자 |

---

## 실행 흐름

```
/analyze → (사용자 검토) → /build → (/fix if needed) → /clean
```

| 단계 | Command | 입력 | 산출물 | 상태 전이 |
|------|---------|------|--------|-----------|
| 분석 | `/analyze` | PPT (1.Prep/) | people_spec + machine_spec | idle → task_analyzed |
| 검토 | 사용자 수동 | final/people_spec.md 수정 | — | — |
| 빌드 | `/build` | people_spec + machine_spec | HTML/XML/Java 코드 + verify_result + deploy(apply) | task_analyzed → task_built |
| 수정 | `/fix` | fix 요청 (fix/1.Prep/) | 패치된 산출물 | task_built → fix_applied |
| 정리 | `/clean` | work/ 전체 | archive/work_{timestamp}/ | → idle |
| 캐시 | `/cache-refresh` | DB | convention cache *.txt | (상태 변경 없음) |

### /build 내부 skill 호출

1. **review** — original vs final people_spec diff → final/machine_spec.yml 생성
2. **generate** — skeleton 치환 → 코드 생성 + verify 정책 기반 검증 + SQL 산출물 생성
3. **deploy** (자동, apply 기본) — deliverables → 실제 프로젝트 경로 배포

deploy는 `verify_result.status in [pass, warn]`, deliverables 존재 시 자동 진입. 기본 모드는 `apply` (실제 파일 복사). deploy 실패는 build 실패가 아님.

---

## 상태 전이

```
idle → task_analyzed → task_built → fix_applied → (clean) → idle
                                  ↘ (clean) → idle
         ↘ (clean) → idle

failed → (clean only) → idle
```

| 상태 | 설명 | 허용 command |
|------|------|-------------|
| `idle` | 초기 상태 | analyze, cache-refresh |
| `task_analyzed` | 분석 완료, 사용자 검토 대기 | build, clean, cache-refresh |
| `task_built` | 코드 생성 완료 | fix, clean, cache-refresh |
| `fix_applied` | 수정 적용 완료 | fix, clean, cache-refresh |
| `failed` | 실패 | clean, cache-refresh |

SSOT: `system/config/runtime.yml` → `state_machine`

---

## 디렉토리 구조

```
gdi_auto_work/
├── .claude/
│   ├── commands/                   # Command (얇은 오케스트레이터)
│   │   ├── analyze.md              #   PPT → people_spec + machine_spec
│   │   ├── build.md                #   review → generate → deploy
│   │   ├── fix.md                  #   오류 분류 → 패치/정책보완
│   │   ├── clean.md                #   work → archive, 초기화
│   │   └── cache-refresh.md        #   convention cache 갱신
│   ├── skills/                     # Skill (정책 기반 실행자)
│   │   ├── review.md               #   people_spec diff → machine_spec
│   │   ├── generate.md             #   skeleton 치환 → 코드 + verify
│   │   ├── deploy.md               #   deliverables → target path
│   │   └── system-fix.md           #   구조적 오류 → 정책 보정
│   └── settings.json
│
├── system/
│   ├── config/
│   │   ├── paths.yml               # 전체 경로 체계 (SSOT)
│   │   ├── runtime.yml             # 상태 전이 + command 계약 + cache 정책
│   │   ├── framework_manifest.yml  # GDI FERP 프레임워크 정의
│   │   └── naming.yml              # 네이밍 규칙 (module_id, SQL ID 등)
│   │
│   ├── schemas/                    # JSON Schema (9개)
│   │   ├── analyze_manifest.schema.json
│   │   ├── build_manifest.schema.json
│   │   ├── fix_manifest.schema.json
│   │   ├── verify_result.schema.json
│   │   ├── deploy_summary.schema.json
│   │   ├── machine_spec.schema.json
│   │   ├── people_spec.schema.json
│   │   ├── active_context.schema.json
│   │   └── framework_manifest.schema.json
│   │
│   ├── policies/
│   │   ├── analyze/  (5)           # PPT 추출, 태그 분류, 화면유형, 보완 우선순위, spec 생성
│   │   ├── framework/ (13)         # 레이아웃, 패턴, skeleton, 금지API, SQL, 코드, 버튼, PostgreSQL
│   │   ├── verify/  (8)            # HTML/XML/SQL/Controller/Service 검증, 자기진단, escalation, 오류분류
│   │   └── runtime/ (5)            # 경로접근, DB접근, 격리, 생명주기, deploy
│   │
│   ├── templates/
│   │   ├── skeletons/              # 코드 생성 골격 (4 artifact × 4 화면유형 = 16개)
│   │   │   ├── html/               #   list, list-detail, form, popup
│   │   │   ├── xml/                #   list, list-detail, form, popup
│   │   │   ├── controller/         #   list, list-detail, form, popup
│   │   │   └── service/            #   list, list-detail, form, popup
│   │   ├── screen-types/           # 화면유형 정의 (4개)
│   │   │   ├── list.yml
│   │   │   ├── list-detail.yml
│   │   │   ├── form.yml
│   │   │   └── popup.yml
│   │   └── spec/                   # spec 템플릿
│   │       ├── people_spec.md
│   │       ├── people_spec_guide.md
│   │       └── machine_spec.yml
│   │
│   ├── cache/convention/           # Convention Cache
│   │   ├── code.txt                # 공통코드 (sy_code_mst/dtl)
│   │   ├── table.txt               # 테이블/컬럼/PK
│   │   ├── view.txt                # 뷰 정의
│   │   ├── function.txt            # 함수/프로시저
│   │   ├── trigger.txt             # 트리거
│   │   └── sql/                    # cache refresh용 SELECT SQL (5개)
│   │
│   └── runtime/                    # 런타임 상태 (실행 시 생성)
│       ├── active/task/
│       ├── active/fix/
│       └── logs/
│
├── work/                           # 활성 작업 영역 (single-active)
│   ├── .active_context.yml         # 현재 실행 상태
│   ├── .lock                       # 동시 실행 방지 (UNLOCKED | LOCKED:{type}:{command})
│   ├── task/
│   │   ├── 1.Prep/                 # 사용자 입력 (PPT)
│   │   ├── 2.Working/
│   │   │   ├── original/           # analyze 원본 (people_spec, machine_spec)
│   │   │   ├── final/              # 확정본 (사용자 수정 people_spec, build machine_spec)
│   │   │   ├── manifests/          # analyze.manifest, build.manifest, verify_result
│   │   │   ├── extracted/          # PPT 추출 결과
│   │   │   ├── classified/         # 화면유형 분류 결과
│   │   │   ├── mapped/             # skeleton 매핑 결과
│   │   │   └── generated/          # 생성된 코드 + SQL 산출물
│   │   └── 3.Result/
│   │       ├── deliverables/       # 최종 산출물
│   │       ├── report/             # build_report.md + deploy_summary.yml
│   │       └── review/             # review 결과
│   └── fix/
│       ├── 1.Prep/                 # fix 요청 파일 (.md, .txt, .png)
│       ├── 2.Working/
│       │   ├── manifests/          # fix.manifest, verify_result
│       │   ├── analyzed/           # fix 분석 결과
│       │   ├── patched/            # 수정된 파일
│       │   └── regenerated/        # 재생성된 파일
│       └── 3.Result/
│           ├── deliverables/       # fix 결과물
│           └── report/             # fix 보고서
│
├── proposals/                      # system-fix 정책 제안
│   └── policy_changes/
│       ├── new/                    # 미적용 제안 (low severity)
│       ├── done/                   # 적용 완료 (high severity)
│       └── backup/                 # 변경 전 backup
│
└── archive/                        # History Only (실행 입력 금지)
    └── work_YYYYMMDD_HHMMSS/       # /clean 시 보관
```

---

## 정책 체계 (31개)

### Analyze 정책 (`system/policies/analyze/`) — 5개

| 파일 | 역할 |
|------|------|
| `ppt_extraction.yml` | GROUP 재귀탐색, 위치기반 영역분류, 라벨/입력 패턴, gravity 추출, 환각방지 (PE-001~007) |
| `classify_tags.yml` | 13개 태그 분류 (META, SEARCH, GRID, FORM, BUTTON 등) + CT-001 |
| `screen_type_rules.yml` | 화면유형 자동 분류 조건 + unknown 수작업 전환 |
| `default_resolution.yml` | 보완 우선순위 5단계 (DDL → 코드정의서 → 화면유형 → 업무규칙 → 에이전트 판단) |
| `spec_generation.yml` | people_spec/machine_spec 생성 규칙, 미확정 처리, cache 연계, 태그 규칙, 원본 데이터 기반 강제 (DSI-001~006) |

### Framework 정책 (`system/policies/framework/`) — 13개

| 파일 | 역할 |
|------|------|
| `layout_rules.yml` | 5칸 gravity 배치, CSS 클래스, data-grid 필수, dataToolbar 필수, 컬럼 균등분배 (LR-001~010) |
| `html_patterns.yml` | Thymeleaf fragment, IIFE+listener, webix.ui 패턴 |
| `xml_patterns.yml` | namespace, SQL ID, 동적SQL, audit 컬럼, ON CONFLICT |
| `controller_patterns.yml` | @AddUserInfo, BaseResponse, 예외처리 패턴 |
| `service_patterns.yml` | @Transactional, mapper 호출, 벌크 최적화 |
| `template_selection.yml` | 화면유형별 skeleton 선택 + confidence 기반 manual fallback |
| `skeleton_contract.yml` | placeholder 계약 + artifact required structure + CG-001~003 |
| `forbidden_apis.yml` | 금지 패턴 (자유 HTML, jQuery AJAX, 전역 함수, SELECT * 등) |
| `sql_generation.yml` | SQL 산출물 생성 조건, 파일명 규칙, 커스텀코드헬프/트리거 SQL, 필수 생성 강제 (SG-001~011) |
| `code_registration.yml` | comm_cd 순수값 규칙, 기존 코드그룹 재사용 금지 (CR-001~005) |
| `button_mapping.yml` | 표준 7 + 비표준 etc{N} 매핑, 본문 섹션 버튼, 자동 교정 (BM-001~004) |
| `code_component_defaults.yml` | 코드콤보/코드헬프 기본값, DATA_VIEW/DATA_CODE_TYPE 상수, 파일컴포넌트, 그리드 컬럼 너비 (CC-001~009) |
| `postgresql_rules.yml` | 집계+윈도우 금지, 중첩집계 금지, STRING_AGG 제한 (PG-001~004) |

### Verify 정책 (`system/policies/verify/`) — 8개

| 파일 | 역할 |
|------|------|
| `html_checks.yml` | HTML 검증 18 critical + 3 warning (IIFE, PGM, fragment, 버튼, 코드콤보 등) |
| `xml_checks.yml` | XML 검증 10 critical + 2 warning (namespace, resultType, audit, 금지컬럼 등) |
| `sql_checks.yml` | SQL 검증 10 critical (메뉴등록, pgm_path, 감사컬럼 8개, execution_order, DDL/코드/코드헬프/트리거 누락 검사) (SC-001~010) |
| `controller_checks.yml` | Controller 검증 6 critical (@Slf4j, 생성자주입, BaseResponse, CommonController 일관성) |
| `service_checks.yml` | Service 검증 6 critical (BaseService, reader/writer, @Transactional, namespace) |
| `self_diagnosis.yml` | 자기진단 6규칙 (DDL보완 제안, 코드등록 제안, 불명확 경고, 자동/수동 구분, 환각 검출, SQL 산출물 누락 검출) (SD-001~006) |
| `escalation_rules.yml` | 오류 escalation (자동수정→재검증, manual_required, system-fix 트리거, build 종료 정책) |
| `issue_classification.yml` | Fix 오류 유형 5분류 + 에러 코드 9카테고리 + patch 판정 기준 |

### Runtime 정책 (`system/policies/runtime/`) — 5개

| 파일 | 역할 |
|------|------|
| `allowed_paths.yml` | command별 읽기/쓰기/금지 경로 + system-fix 쓰기 권한 |
| `db_access.yml` | DB 접속정보, cache-first 원칙, fallback 조건/절차, 기록 규칙 (DB-001~006) |
| `context_isolation.yml` | task/fix 격리, lock 형식, active_context 갱신 시점 |
| `lifecycle.yml` | 필수 산출물 검증, unresolved 정책, failed 복구, zone 규칙 |
| `deploy_policy.yml` | deploy 모드(apply 기본/prepare-only), 진입조건, target path 규칙, 충돌처리 |

---

## Manifest 체계

| Manifest | 생성자 | 주요 필드 | Schema |
|----------|--------|-----------|--------|
| `analyze.manifest.yml` | /analyze | status, screens[], cache, unresolved | `analyze_manifest.schema.json` |
| `build.manifest.yml` | /build | review_summary, artifact_plan, verify(critical_pass, details, self_diagnosis), cache, deploy | `build_manifest.schema.json` |
| `verify_result.yml` | /build, /fix | checks[], critical_pass, manual_required, auto_fixed[], details(5유형), self_diagnosis | `verify_result.schema.json` |
| `deploy_summary.yml` | /build 선택적 | mode, target_paths[], warnings[], conflicts[] | `deploy_summary.schema.json` |
| `fix.manifest.yml` | /fix | issue_type, error_classification, system_fix, verify, cache | `fix_manifest.schema.json` |

---

## 코드 생성 파이프라인

```
PPT
 ↓ ppt_extraction.yml (PE-001~006)
 ↓ classify_tags.yml
 ↓ screen_type_rules.yml
화면 분류 결과
 ↓ spec_generation.yml + default_resolution.yml
people_spec.md + machine_spec.yml (original)
 ↓ (사용자 검토/수정)
people_spec.md (final)
 ↓ review skill — diff 분석
machine_spec.yml (final)
 ↓ template_selection.yml → skeleton 선택
 ↓ skeleton_contract.yml → placeholder 완전성 검증
 ↓ skeleton + {{placeholder}} 치환
   ├── code_component_defaults.yml (코드콤보/코드헬프)
   ├── button_mapping.yml (버튼 → listener)
   ├── layout_rules.yml (5칸 gravity)
   └── forbidden_apis.yml (금지 패턴 차단)
생성된 코드 (HTML/XML/Controller/Service)
 ↓ sql_generation.yml → 메뉴등록/DDL보완/코드등록 SQL
 ↓ verify 정책 (html/xml/sql/controller/service_checks.yml)
 ↓ self_diagnosis.yml → DDL/코드 미등록 자동 탐지
 ↓ escalation_rules.yml → auto_fix / manual_required / system-fix
verify_result.yml
 ↓ critical_pass → deliverables/
 ↓ deploy_policy.yml → target path 계산 + apply (실제 파일 복사)
deploy_summary.yml
```

---

## Convention Cache

| 파일 | 내용 | 갱신 시점 |
|------|------|-----------|
| `code.txt` | 공통코드 (sy_code_mst/dtl) | /analyze, /cache-refresh |
| `table.txt` | 테이블/컬럼/PK | /analyze, /cache-refresh |
| `view.txt` | 뷰 정의 | /analyze, /cache-refresh |
| `function.txt` | fn_*/sp_* 함수/프로시저 | /analyze, /cache-refresh |
| `trigger.txt` | 트리거 정의 | /analyze, /cache-refresh |

**전략**: cache-first → cache miss/insufficient/mismatch → DB fallback (SELECT only) → manifest 기록.
**SSOT**: `system/policies/runtime/db_access.yml`

### 컬럼명 해석 절차 (CR-001)

PPT 한글 필드 라벨 → 실제 DB 컬럼명 매핑 시:
1. **Cache table.txt** 추론 매칭 (한글 주석, 부분 일치, 약어 패턴)
2. **DB Fallback** 직접 조회 (cache miss 시)
3. **DDL 보완 SQL 생성** (필수 컬럼인데 DB 미존재 시)
- 에이전트가 컬럼명을 임의 생성하는 것을 **절대 금지**
- SSOT: `system/policies/analyze/default_resolution.yml` → `column_resolution`

### 프레임워크 소스 Fallback

정책/cache에서 구현 방법을 찾을 수 없을 때:
1. 정책 파일 확인 → 2. convention cache 확인 → 3. **프레임워크 소스 검색** → 4. 패턴 검증 (최소 2개 파일)
- GDI 프레임워크: `C:/gdi/src/main/`
- FERP 레퍼런스: `C:/GitLap/FERP/FERP(TOBE)/ferp-backoffice/src/main/`
- SSOT: `system/config/framework_manifest.yml` → `framework_source_fallback`

---

## 화면 유형

| 유형 | 설명 | 필수 산출물 | Controller/Service |
|------|------|-------------|-------------------|
| **list** | 검색 + 목록 그리드 | HTML, XML | CommonController (기본) |
| **list-detail** | 목록 + 상세/편집 | HTML, XML | CG-002 조건 시 Custom |
| **form** | 단건 입력/수정 폼 | HTML, XML | CG-002 조건 시 Custom |
| **popup** | 모달 팝업 | HTML | 불필요 |

CG-002 (Custom 필수): 암호화, 복합 로직, 외부 API, 프로시저 호출, 다중 테이블 트랜잭션
CG-003 (Common 충분): 단순 CRUD, saveAll, 코드헬프

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| 대상 프레임워크 | GDI FERP Backoffice (Spring Boot + Thymeleaf + MyBatis + Webix) |
| DB | PostgreSQL |
| 언어 | Java 17+ (Jakarta) |
| 프론트엔드 | Webix UI + jQuery + Bootstrap 5 |
| 자동화 도구 | Claude Code (.claude/commands + skills) |
| Base 클래스 | BaseController, BaseService, BaseParm, BaseResponse |
| 공통 서비스 | CommonController, CommonService, FileService |

---

## 빠른 시작

```bash
# 1. Convention cache 갱신 (최초 1회)
/cache-refresh

# 2. PPT 배치
# work/task/1.Prep/ 에 화면설계서.pptx 배치

# 3. 분석
/analyze

# 4. 사용자 검토
# work/task/2.Working/final/people_spec.md 확인/수정

# 5. 빌드
/build

# 6. 문제 발견 시 수정
# work/fix/1.Prep/ 에 fix 요청 배치
/fix

# 7. 완료 후 정리
/clean
```

---

## SSOT (진실원천) 맵

| 관심사 | SSOT 위치 | 소비자 |
|--------|----------|--------|
| 시스템 규칙 | `system/policies/**/*.yml` (31개) | command, skill |
| 실행 계약 / 상태 전이 | `system/config/runtime.yml` | command |
| 경로 체계 | `system/config/paths.yml` | 전체 |
| 네이밍 규칙 | `system/config/naming.yml` | analyze, review, generate |
| 프레임워크 정의 | `system/config/framework_manifest.yml` | deploy, generate |
| 산출물 구조 | `system/schemas/*.json` (9개) | manifest 검증 |
| 생성 골격 | `system/templates/skeletons/**` (16개) | generate |
| 화면유형 정의 | `system/templates/screen-types/*.yml` (4개) | analyze, review, generate |

---

## 시스템 설계 철학

이 시스템은 **통제형 워크플로우 엔진**이다.

| 관점 | 설계 |
|------|------|
| 통제 전략 | policy-first, schema-first, stateful control |
| 생성 방식 | constrained generation — skeleton + placeholder 치환만 허용 |
| Context 관리 | single-active + archive 차단 + spec 이원화 기반 격리 |
| 루프 구조 | 사용자 검토 루프 + fix 루프 + 선택적 deploy 루프 |
| 지식 접근 | LLM 기억보다 외부 근거(cache, DDL, 정책 파일) 우선 |
| Manifest | 실행 계약이자 단계별 상태 증거물 |
| 인간 개입 | analyze 후 spec 검토, deploy apply 전 확인, fix 후 정책 보정 판단 |
| 운영 철학 | autonomous agent가 아니라 **constrained orchestration engine** |

> AI를 통제 가능한 공정 안에 넣어 다루는 시스템.
> 자유도를 줄이는 대신 재현성, 추적성, 품질 일관성을 확보한다.

---

## 변경 시 동기화 가이드

### 정책 추가
1. `system/policies/{domain}/` 에 yml 생성
2. 해당 command/skill의 "필수 정책 로드" 테이블에 추가
3. 이 README 정책 체계 테이블에 추가

### Command 추가
1. `.claude/commands/` 에 md 생성
2. `system/config/runtime.yml` → `commands` 섹션에 계약 추가
3. `system/policies/runtime/allowed_paths.yml`에 경로 규칙 추가
4. `.claude/settings.json` → `workflow.commands`에 등록
5. 이 README 반영

### Manifest 필드 추가
1. `system/schemas/*.schema.json` 수정
2. command/skill의 기록 구조 확인
3. 이 README manifest 체계 테이블 확인

---

## 프로젝트 이력

| 단계 | 산출물 |
|------|--------|
| 프레임워크 분석 | `framework_analysis_report.md` |
| Skeleton 추출 | `skeleton_extraction_report.md` |
| Manifest 계약화 | `manifest_contract_report.md` |
| Command 구현 | `command_implementation_report.md` |
| v5 이식 | `migration_report_v1.md` |
| Cache 통합 | `cache_integration_report.md` |
| DB Fallback 통합 | `cache_db_fallback_integration_report.md` |
| Build-Deploy 연결 | `build_deploy_integration_report.md` |
| v5 잔존 정책 이관 | `migration_report_v2.md` |
| 정책 중심 리팩토링 | `refactoring_report_v1.md` |
| 시스템 정합성 정비 | `consistency_report_v1.md` |
