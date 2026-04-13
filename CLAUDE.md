# GDI Auto Work - Claude Code 지시서

## 핵심 원칙: 추론 금지, 정책 우선

**이 시스템의 모든 작업은 정책 파일과 convention cache에 근거해야 한다. 에이전트의 추론/추정으로 작업하는 것을 절대 금지한다.**

---

## 1. Cache-First 원칙 (최우선 규칙)

모든 데이터 참조는 반드시 `system/cache/convention/` 캐시를 먼저 확인한다.

| 캐시 파일 | 용도 |
|---|---|
| `table.txt` | 테이블/컬럼 DDL 정의 (컬럼명 확정의 유일한 근거) |
| `code.txt` | 코드정의서 (wrk_tp_cd, cd_tp_cd, comm_cd) |
| `view.txt` | 뷰 정의 |
| `function.txt` | 함수 정의 |
| `trigger.txt` | 트리거 정의 |

**위반 금지 사항:**
- cache를 읽지 않고 컬럼명을 추론하는 행위
- cache에 없는 컬럼명을 에이전트가 만들어내는 행위
- cache 확인 없이 SQL을 생성하는 행위

**cache에서 못 찾으면:** DB fallback 조회 (`system/policies/runtime/db_access.yml` 절차 따름). DB에서도 못 찾으면 DDL 보완 SQL 생성 또는 unresolved 처리. 절대 추론하지 않는다.

---

## 2. 컬럼명 해석 절차 (Column Resolution) - 절대 규칙

PPT 한글 라벨 → DB 컬럼명 매핑 시 반드시 아래 순서를 따른다:

1. **Step 1**: `system/cache/convention/table.txt` 에서 대상 테이블 컬럼 조회 → 한글 주석과 매칭
2. **Step 2**: cache에서 실패 시 DB fallback SELECT 조회
3. **Step 3**: DB에서도 실패 + 화면에 필수인 경우 → DDL 보완 SQL 생성

**금지:** 에이전트가 "이 컬럼명이 맞을 것 같다"고 추론하여 컬럼명을 생성하는 것. 출처 없는 컬럼명은 machine_spec에 사용 금지.

상세: `system/policies/analyze/default_resolution.yml` CR-001

---

## 3. 명령(Command) 실행 전 필수 읽기 정책

각 command 실행 시 반드시 해당 정책 파일을 먼저 읽어야 한다. 건너뛰기 금지.

### /analyze 실행 전 필수 읽기
- `system/config/runtime.yml` (상태 전이, 실행 조건)
- `system/policies/analyze/ppt_extraction.yml`
- `system/policies/analyze/classify_tags.yml`
- `system/policies/analyze/screen_type_rules.yml`
- `system/policies/analyze/spec_generation.yml`
- `system/policies/analyze/default_resolution.yml` (컬럼 해석 절차)
- `system/cache/convention/table.txt` (cache refresh 후)
- `system/cache/convention/code.txt` (cache refresh 후)

### /build 실행 전 필수 읽기
- `system/config/runtime.yml` (상태 전이)
- `system/policies/framework/skeleton_contract.yml` (placeholder 계약)
- `system/policies/framework/sql_generation.yml` (SQL 산출물 규칙)
- `system/policies/framework/forbidden_apis.yml` (금지 API/패턴)
- `system/policies/framework/html_patterns.yml`
- `system/policies/framework/xml_patterns.yml`
- `system/policies/framework/controller_patterns.yml`
- `system/policies/framework/service_patterns.yml`
- `system/policies/framework/layout_rules.yml`
- `system/policies/framework/code_registration.yml`
- `system/policies/framework/button_mapping.yml`
- `system/policies/framework/code_component_defaults.yml`
- `system/policies/verify/self_diagnosis.yml`
- `system/policies/verify/escalation_rules.yml`
- `system/cache/convention/table.txt`
- `system/cache/convention/code.txt`

### /fix 실행 전 필수 읽기
- `system/config/runtime.yml` (상태 전이)
- `system/policies/verify/issue_classification.yml`
- fix 대상에 해당하는 build 정책 파일들
- `system/cache/convention/table.txt`
- `system/cache/convention/code.txt`

---

## 4. 상태 전이 모델

`work/.active_context.yml`의 phase를 반드시 확인하고 허용된 command만 실행한다.

```
idle → analyze → task_analyzed → build → task_built → fix → fix_applied → clean → idle
```

상태별 허용 command는 `system/config/runtime.yml`의 `state_machine` 섹션에 정의되어 있다. 위반 금지.

---

## 5. SQL 생성 시 필수 규칙

### sy_pgm_info (메뉴등록)
- PK는 `pgm_id`만. `comp_cd` 컬럼 없음 — INSERT/ON CONFLICT에서 comp_cd 사용 금지
- `etc_use_yn` 컬럼 미존재 — `etc_desc{N}`만 사용
- 상세: `system/policies/framework/sql_generation.yml` SG-001

### sy_menu_info (메뉴등록)
- `menu_lvl`, `sort_ord`, `use_yn` 컬럼 미존재 — 사용 금지
- 컬럼 목록은 반드시 `table.txt`의 DDL과 대조
- 상세: `system/policies/framework/sql_generation.yml` SG-001

### 감사컬럼
- 정확한 이름: `firs_reg_pgm_id`, `firs_reg_dts`, `firs_reg_user_id`, `firs_reg_ip`, `fina_reg_pgm_id`, `fina_reg_dts`, `fina_reg_user_id`, `fina_reg_ip`
- 금지 변형: `frst_reg`, `first_reg`, `fist_reg`, `fnl_reg` 등 추론 약어

### comp_cd 규칙
- `comp_cd` 컬럼이 실제로 존재하는 테이블에서만 WHERE 조건에 추가
- `table.txt`에서 comp_cd 컬럼 존재 여부를 반드시 확인 후 판단

---

## 6. 금지 API 패턴 (에이전트 추론 생성 방지)

`platform.*` 중 허용되는 것만 사용:
- `platform.post()`, `platform.url.*` (select, selectOne, save, saveAll, procedure), `platform.listener[PGM]`

**아래는 모두 존재하지 않는 API — 사용 절대 금지:**
- `platform.alert()` → `popup.alert.show()` 사용
- `platform.confirm()` → `popup.confirm.show()` 사용
- `platform.dataSearchForm()`, `platform.dataForm()`, `platform.dataGrid()`, `platform.dataToolbar()` → `webix.ui({...})` 사용
- `platform.getFormValues()`, `platform.setFormValues()`, `platform.setFormReadonly()` → `listener.form.xxx.getData()/.setData()/.setReadonly()` 사용
- `platform.openPopup()` → `DataModalNew + DataModalConfig` 패턴 사용

전체 목록: `system/policies/framework/forbidden_apis.yml`

---

## 7. Controller/Service 생성 규칙

**기본:** CommonController/CommonService 사용. 커스텀 생성하지 않음.
**예외 (커스텀 필요 시):** 암호화, 복잡한 비즈니스 로직, 외부 API, 파일 처리, 프로시저 후처리, 다중 테이블 조건부 처리
상세: `system/policies/framework/skeleton_contract.yml` CG-001~CG-003

---

## 8. 보완 우선순위 (Resolution Priority)

정보가 부족할 때 참조하는 순서:

1. **DDL** (`table.txt`) — 최우선
2. **코드정의서** (`code.txt`)
3. **화면유형 기본규칙** (`system/templates/screen-types/*.yml`)
4. **일반 업무규칙** (`system/config/naming.yml`, `system/policies/framework/*.yml`)
5. **에이전트 판단** — 최후 수단, 반드시 `[추론/보완 항목]` 태그 부착

에이전트 판단은 위 1~4에서 정보를 얻을 수 없을 때만 허용된다.

---

## 9. Verify (자기진단) 필수 수행

build/fix 완료 시 반드시 자기진단을 수행한다:
- DDL 미존재 컬럼 사용 검출 (SD-001)
- 미등록 코드 검출 (SD-002)
- 환각(hallucination) 검출 — PPT 원본과 spec 불일치 (SD-005)
- SQL 산출물 누락 검출 (SD-006)
- 금지 API 사용 검출

상세: `system/policies/verify/self_diagnosis.yml`, `system/policies/verify/escalation_rules.yml`

---

## 10. 작업 디렉토리 규칙

- **읽기 전용:** `system/**` (정책/설정), `archive/**` (이력)
- **쓰기 가능:** `work/task/**` (analyze/build), `work/fix/**` (fix)
- **archive는 실행 입력이 아님** — archive를 참조하여 작업하는 것 금지
- 상세: `system/policies/runtime/lifecycle.yml`, `system/policies/runtime/allowed_paths.yml`

---

## 11. DB 접속 규칙

- **SELECT만 허용.** INSERT/UPDATE/DELETE/DDL 금지
- cache-first 후 부족 시에만 DB fallback
- fallback 사용 시 manifest에 기록 필수
- 상세: `system/policies/runtime/db_access.yml`
