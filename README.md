# GDI Auto Work

GDI FERP Backoffice 화면 자동 생성 시스템.
PPT 화면설계서를 입력으로 받아, 프레임워크 규칙에 따라 HTML/XML/Controller/Service 코드를 자동 생성한다.

---

## 핵심 사상

### 1. Skeleton 기반 생성 (자유 생성 금지)
코드를 처음부터 작성하지 않는다. 프레임워크 분석에서 추출한 **skeleton 템플릿**에 placeholder를 치환하여 산출물을 만든다. skeleton에 정의되지 않은 구조는 생성하지 않는다.

### 2. Dual-Spec 구조
사람이 읽는 문서와 기계가 읽는 입력을 분리한다.
- **people_spec.md** — 사람 검토용. 화면 개요, 필드, 버튼, 비즈니스 규칙을 자연어로 기술
- **machine_spec.yml** — build 입력용. screen_type, skeleton_choice, placeholders, SQL 정보를 구조화

analyze가 둘 다 생성하고, 사용자가 people_spec을 검토/수정하면, build가 변경분을 machine_spec에 반영하여 최종 코드를 만든다.

### 3. Single-Active Workspace
동시에 하나의 task 또는 fix만 실행한다. 완료되면 clean으로 archive에 보관하고, 새 작업을 시작한다.

### 4. Cache-First 원칙
DB 코드/테이블/뷰/함수 정보는 `system/cache/convention/`에 캐시한다. 모든 조회는 cache를 먼저 확인하고, 부족할 때만 DB에 SELECT 조회(fallback)한다.

### 5. Manifest 기반 입출력
모든 command는 실행 전후로 manifest(YAML)를 남긴다. 무엇을 입력으로 읽었고, 무엇을 산출했고, 어디서 실패했는지 기계적으로 추적 가능하다.

### 6. Archive = History Only
`archive/`는 과거 작업 이력 보관소이다. 어떤 command도 archive를 실행 입력으로 사용하지 못한다.

---

## 실행 흐름

```
/analyze → (사용자 검토) → /build → (/fix if needed) → /clean
```

| 단계 | Command | 입력 | 산출물 | 상태 전이 |
|------|---------|------|--------|-----------|
| 분석 | `/analyze` | PPT (1.Prep/) | people_spec, machine_spec (original+final) | idle → task_analyzed |
| 검토 | 사용자 수동 | final/people_spec.md 수정 | — | — |
| 빌드 | `/build` | people_spec + machine_spec | final/machine_spec + HTML/XML/Java 코드 | task_analyzed → task_built |
| 수정 | `/fix` | fix 요청 (fix/1.Prep/) | 패치된 산출물 | task_built → fix_applied |
| 정리 | `/clean` | work/ 전체 | archive/work_{timestamp}/ | → idle |
| 캐시 | `/cache-refresh` | DB | convention cache *.txt | (상태 변경 없음) |

### /build 내부 구조
build는 두 개의 skill을 순서대로 호출한다:
1. **review skill** — original vs final people_spec diff 분석 → **final/machine_spec.yml 생성**
2. **generate skill** — final/machine_spec.yml → skeleton 치환 → 코드 생성 + self-check

build 이후 모든 단계는 **final/machine_spec.yml만을 기준**으로 진행한다.

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
| `idle` | 초기 상태 | analyze |
| `task_analyzed` | 분석 완료, 사용자 검토 대기 | build, clean |
| `task_built` | 코드 생성 완료 | fix, clean |
| `fix_applied` | 수정 적용 완료 | fix, clean |
| `failed` | 실패 | clean (후 재시작) |

---

## 디렉토리 구조

```
gdi_auto_work/
├── .claude/
│   ├── commands/          # Claude Code 실행 command
│   │   ├── analyze.md
│   │   ├── build.md
│   │   ├── fix.md
│   │   ├── clean.md
│   │   └── cache-refresh.md
│   ├── skills/            # Command가 호출하는 하위 skill
│   │   ├── review.md      # people_spec diff → machine_spec 생성
│   │   ├── generate.md    # skeleton 치환 → 코드 생성
│   │   ├── deploy.md      # 산출물 배포 준비
│   │   └── system-fix.md  # 구조적 오류 → 정책/skeleton 보정 제안
│   └── settings.json
│
├── system/
│   ├── config/
│   │   ├── paths.yml               # 경로 체계
│   │   ├── runtime.yml             # 상태 전이, cache 정책, command 계약
│   │   ├── framework_manifest.yml  # 프레임워크 분석 결과
│   │   └── naming.yml              # 네이밍 규칙
│   │
│   ├── cache/convention/           # Convention Cache
│   │   ├── code.txt                # 공통코드 정의
│   │   ├── table.txt               # 테이블/컬럼 정의
│   │   ├── view.txt                # 뷰 정의
│   │   ├── function.txt            # 함수/프로시저 정의
│   │   ├── trigger.txt             # 트리거 정의
│   │   └── sql/                    # cache refresh용 SELECT SQL
│   │
│   ├── templates/
│   │   ├── skeletons/              # 코드 생성 골격 (html/xml/controller/service × 4유형)
│   │   ├── screen-types/           # 화면유형 정의 (list/list-detail/form/popup)
│   │   └── spec/                   # people_spec/machine_spec 템플릿
│   │
│   ├── schemas/                    # JSON Schema (manifest/spec/context 검증)
│   │
│   └── policies/
│       ├── analyze/                # PPT 추출, 태그 분류, 화면유형 판정
│       ├── framework/              # 레이아웃, 패턴, skeleton 계약, 금지 API
│       ├── runtime/                # 경로 접근, DB 접근, 격리, 생명주기
│       └── verify/                 # self-check 규칙
│
├── work/                           # 활성 작업 영역
│   ├── .active_context.yml         # 현재 실행 상태
│   ├── .lock                       # 동시 실행 방지 lock
│   ├── task/
│   │   ├── 1.Prep/                 # 사용자 입력 (PPT)
│   │   ├── 2.Working/              # 중간 산출물
│   │   │   ├── original/           # analyze 원본 (people_spec, machine_spec)
│   │   │   ├── final/              # 확정본 (사용자 수정 people_spec, build machine_spec)
│   │   │   ├── manifests/          # analyze/build manifest + verify_result
│   │   │   ├── extracted/          # PPT 추출 결과
│   │   │   ├── classified/         # 화면유형 분류 결과
│   │   │   ├── mapped/             # skeleton 매핑 결과
│   │   │   └── generated/          # 생성된 코드
│   │   └── 3.Result/
│   │       ├── deliverables/       # 최종 산출물
│   │       └── report/             # 빌드 보고서
│   └── fix/
│       ├── 1.Prep/                 # fix 요청 파일
│       ├── 2.Working/              # fix 중간 산출물
│       └── 3.Result/               # fix 결과
│
└── archive/                        # History Only (실행 입력 금지)
    └── work_YYYYMMDD_HHMMSS/       # clean 시 보관
```

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| 대상 프레임워크 | GDI FERP Backoffice (Spring Boot + Thymeleaf + MyBatis + Webix) |
| DB | PostgreSQL (10.10.1.100:5466/GDI_SERVICE) |
| 언어 | Java 17+ (Jakarta) |
| 템플릿 | Thymeleaf fragment 기반 |
| ORM | MyBatis XML Mapper |
| 프론트엔드 | Webix UI + jQuery + Bootstrap 5 |
| 자동화 도구 | Claude Code (.claude/commands + skills) |

---

## 화면 유형

| 유형 | 설명 | 필수 산출물 | 대표 샘플 |
|------|------|-------------|-----------|
| **list** | 검색 + 목록 그리드 | HTML, XML | DTA010 |
| **list-detail** | 목록 + 상세/편집 (좌우분할, 탭) | HTML, XML | DTA020, DTJ010 |
| **form** | 단건 입력/수정 폼 | HTML, XML | (서브화면) |
| **popup** | 모달 팝업 | HTML | DTA030_P01 |

Controller/Service는 CommonController로 충분하면 생성하지 않는다. 엑셀 업로드, 서버 검증, 복잡 트랜잭션 등 CG-002 조건에 해당할 때만 생성한다.

---

## Skeleton 기반 생성 원리

```
people_spec.md (사용자 검토)
       ↓ diff
machine_spec.yml (final)
       ↓
template_selection.yml → skeleton 선택 (html/xml/controller/service)
       ↓
skeleton_contract.yml → placeholder 완전성 검증
       ↓
skeleton 파일 + {{placeholder}} 치환 → 코드 생성
       ↓
forbidden_apis.yml + verify_result → self-check
       ↓
deliverables/
```

**placeholder 형식**: `{{name}}` (예: `{{screen_id}}`, `{{search_fields}}`, `{{grid_columns}}`)

---

## Convention Cache

```
analyze 시작 → 무조건 cache refresh (psql → *.txt)
       ↓
build/fix → cache 읽기 전용 참조
       ↓
cache miss → DB fallback (SELECT only) → cache 보강
```

| 파일 | 내용 | 갱신 시점 |
|------|------|-----------|
| `code.txt` | 공통코드 (sy_code_mst/dtl) | /analyze, /cache-refresh |
| `table.txt` | 테이블/컬럼/PK | /analyze, /cache-refresh |
| `view.txt` | 뷰 정의 | /analyze, /cache-refresh |
| `function.txt` | fn_*/sp_* 함수/프로시저 | /analyze, /cache-refresh |
| `trigger.txt` | 트리거 정의 | /analyze, /cache-refresh |

---

## 정책 체계

### Framework 정책 (`system/policies/framework/`)
| 파일 | 역할 |
|------|------|
| `layout_rules.yml` | 5칸 gravity 배치, CSS 클래스, data-grid 필수 |
| `html_patterns.yml` | Thymeleaf fragment, IIFE+listener, webix.ui 패턴 |
| `xml_patterns.yml` | namespace, SQL ID, 동적SQL, audit 컬럼, ON CONFLICT |
| `controller_patterns.yml` | @AddUserInfo, BaseResponse, 예외처리 패턴 |
| `service_patterns.yml` | @Transactional, mapper 호출, 벌크 최적화 |
| `template_selection.yml` | 화면유형별 skeleton 조합 선택 + confidence 기반 manual fallback |
| `skeleton_contract.yml` | placeholder 계약 + artifact required structure + CG-001~003 |
| `forbidden_apis.yml` | 금지 패턴 (자유 HTML, jQuery AJAX, 전역 함수, SELECT * 등) |

### Analyze 정책 (`system/policies/analyze/`)
| 파일 | 역할 |
|------|------|
| `ppt_extraction.yml` | GROUP 재귀탐색, 위치기반 영역분류, 라벨/입력 패턴, gravity 추출 |
| `classify_tags.yml` | 13개 태그 분류 (META, SEARCH, GRID, FORM, BUTTON 등) |
| `screen_type_rules.yml` | 화면유형 자동 분류 조건 + unknown 수작업 전환 |

### Runtime 정책 (`system/policies/runtime/`)
| 파일 | 역할 |
|------|------|
| `allowed_paths.yml` | command별 읽기/쓰기/금지 경로 |
| `db_access.yml` | DB 접속정보, cache-first 원칙, fallback 규칙 |
| `context_isolation.yml` | task/fix 격리, lock 형식, active_context 갱신 시점 |
| `lifecycle.yml` | 필수 산출물 검증, unresolved 정책, failed 복구 |

---

## Manifest 체계

모든 command는 manifest를 생성하여 실행 이력을 기록한다.

| Manifest | 생성 command | 주요 필드 |
|----------|-------------|-----------|
| `analyze.manifest.yml` | /analyze | status, screens[], cache refresh 결과, unresolved |
| `build.manifest.yml` | /build | status, selected_templates, artifact_plan, cache 사용 현황 |
| `verify_result.yml` | /build, /fix | checks[], cache_checks[], warnings[], errors[] |
| `fix.manifest.yml` | /fix | issue_type, impact_scope, cache mismatch, actions_taken |

각 manifest는 `system/schemas/*.schema.json`으로 구조 검증 가능하다.

---

## 빠른 시작

```bash
# 1. Convention cache 갱신 (최초 1회 또는 필요 시)
/cache-refresh

# 2. PPT를 work/task/1.Prep/ 에 배치
cp 화면설계서.pptx work/task/1.Prep/

# 3. 분석 실행
/analyze

# 4. 사용자가 final/people_spec.md 검토/수정

# 5. 빌드 실행 (review + generate)
/build

# 6. 문제 있으면 fix 요청 배치 후 수정
cp fix_request.md work/fix/1.Prep/
/fix

# 7. 완료 후 정리
/clean
```

---

## 프로젝트 이력

| 단계 | 산출물 |
|------|--------|
| 프레임워크 분석 | `framework_analysis_report.md` |
| Skeleton 추출 | `skeleton_extraction_report.md` |
| Manifest 계약화 | `manifest_contract_report.md` |
| Command 구현 | `command_implementation_report.md` |
| v5 이식 | `migration_report_v1.md`, `migration_todo_v1.md` |
| Cache 통합 | `cache_integration_report.md` |
| DB Fallback 통합 | `cache_db_fallback_integration_report.md` |
