# 프레임워크 분석 보고서

## 1. 분석 개요

본 문서는 GDI FERP Backoffice 프레임워크를 분석하여, 새 시스템의 화면 자동 생성을 위한 규칙과 골격을 추출한 결과물이다.
분석 범위는 프레임워크 루트 구조, 레퍼런스 화면군(dt 모듈), 그리고 이들 사이의 연결 구조이다.

- **분석 일자**: 2026-04-05
- **분석 단계**: 프레임워크 분석 및 정책/계약 초안 작성 (코드 생성 전 단계)
- **분석 방법**: 실제 파일 구조/클래스/XML/HTML 직접 분석. 추정 규칙 작성 금지.

---

## 2. 분석 대상 경로

| 대상 | 경로 | 설명 |
|------|------|------|
| 프레임워크 | `C:\gdi\src\main` | GDI 프레임워크 전체 (Java, Resources, Templates, Static) |
| 레퍼런스 Controller/Service | `C:\GitLap\FERP\FERP(TOBE)\ferp-backoffice\src\main\java\com\sjinc\sjerp\proj\dt` | dt 모듈 전체 (31개 Java 파일) |
| 레퍼런스 XML Mapper | `C:\GitLap\FERP\FERP(TOBE)\ferp-backoffice\src\main\resources\mapper\sjerp\dt` | dt 모듈 전체 (97개 XML 파일) |
| 레퍼런스 HTML Template | `C:\GitLap\FERP\FERP(TOBE)\ferp-backoffice\src\main\resources\templates\project\dt` | dt 모듈 전체 HTML 템플릿 |

---

## 3. 프레임워크 핵심 구조

### 3.1 기술 스택

| 항목 | 기술 | 근거 |
|------|------|------|
| 언어 | Java 17+ | Jakarta 패키지 사용 (`jakarta.servlet`, `jakarta.validation`) |
| 프레임워크 | Spring Boot | `@Controller`, `@Service`, `@RequestMapping` |
| 템플릿 | Thymeleaf | `th:fragment`, `th:insert`, `th:replace` |
| ORM | MyBatis | `SqlSessionTemplate`, XML Mapper |
| DB | PostgreSQL | `application-dev.properties`: `jdbc:postgresql://10.10.1.100:5466/GDI_SERVICE` |
| 프론트엔드 UI | Webix | `webix.ui()`, `datagrid`, `dataForm` |
| CSS | Bootstrap 5 | `frame/header.html`에서 로드 |
| JS | jQuery | `frame/script.html`에서 로드 |

### 3.2 패키지 구조

```
com.sjinc.frame          # 프레임워크 핵심 (annotation, aop, db, mvc, utils 등)
com.sjinc.proj.base      # 프로젝트 기반 클래스 (BaseController, BaseService, BaseParm, BaseResponse)
com.sjinc.proj.common    # 공통 데이터 서비스 (CommonController, CommonService)
com.sjinc.proj.ui        # 화면 진입 컨트롤러 (UiController)
com.sjinc.proj.{module}  # 업무 모듈 (sy, dt, etc.)
```

근거: `C:\gdi\src\main\java\com\sjinc\` 디렉토리 전체 탐색

### 3.3 핵심 기반 클래스

| 클래스 | 역할 | 경로 |
|--------|------|------|
| `BaseController` | 모든 Controller 조상 (빈 클래스, @Slf4j) | `proj/base/BaseController.java` |
| `BaseService` | 모든 Service 조상 (빈 클래스) | `proj/base/BaseService.java` |
| `BaseParm` | 요청 파라미터 (HashMap 확장, statement 포함) | `proj/base/BaseParm.java` |
| `BaseParmAll` | 다건 저장용 파라미터 (statement + data) | `proj/base/BaseParmAll.java` |
| `BaseResponse` | 응답 래퍼 (resultCode, resultMessage, resultData) | `proj/base/BaseResponse.java` |
| `CommonController` | 범용 CRUD 엔드포인트 제공 | `proj/common/CommonController.java` |
| `CommonService` | 범용 데이터 접근 (SqlSessionTemplate 래핑) | `proj/common/CommonService.java` |
| `UiController` | 화면 진입 (/ui, /view) | `proj/ui/UiController.java` |

### 3.4 공통 엔드포인트 (CommonController)

| URL | 용도 | 프론트 호출 |
|-----|------|------------|
| `/common/select` | 목록 조회 | `platform.post(platform.url.select, param, callback)` |
| `/common/selectOne` | 단건 조회 | `platform.post(platform.url.selectOne, ...)` |
| `/common/save` | 단건 저장 | `platform.post(platform.url.save, ...)` |
| `/common/saveList` | 리스트 저장 | `platform.post(platform.url.saveList, ...)` |
| `/common/saveAll` | 다중 statement 일괄 저장 | `platform.post(platform.url.saveAll, ...)` |
| `/common/selectPager` | 페이징 조회 | `platform.post(platform.url.selectPager, ...)` |
| `/common/procedure` | 프로시저 호출 | `platform.post(platform.url.procedure, ...)` |

**핵심 발견**: 단순 CRUD는 전용 Controller 없이 CommonController만으로 처리 가능.
프론트에서 `statement: "namespace.sqlId"` 를 직접 지정하여 호출.

근거: `C:\gdi\src\main\java\com\sjinc\proj\common\CommonController.java` (line 71~289)

### 3.5 화면 진입 구조

```
[사이드바 메뉴 클릭]
  → POST /ui { pgm_id: "DTA030" }
  → UiController.ui()
    → DB에서 프로그램 정보 조회 (ui.selectPgm)
    → DB에서 권한 조회 (ui.selectPgmAuthAll)
    → model에 PGM, PGM_INFO, PGM_AUTH, PARAM 세팅
    → return "project" + pgm.get("pgm_path")  // 예: "project/dt/dta/DTA030"
  → Thymeleaf가 templates/project/dt/dta/DTA030.html 렌더링

[팝업 호출]
  → POST /view { view_id: "popupId", view_path: "/dt/dta/DTA030_P01" }
  → UiController.modal()
    → model에 PGM 세팅
    → return "project" + view_path
```

근거: `C:\gdi\src\main\java\com\sjinc\proj\ui\UiController.java` (line 33~66)

---

## 4. 화면 1건의 실제 연결 구조

### 샘플 1: DTA030 (배분유형 관리) - 목록 + 편집 + 엑셀업로드

| 아티팩트 | 파일 | 역할 |
|----------|------|------|
| HTML | `templates/project/dt/dta/DTA030.html` | 검색폼 + 그리드 + 편집 |
| HTML (팝업) | `templates/project/dt/dta/DTA030_P01.html` | 조건 설정 팝업 |
| XML | `mapper/sjerp/dt/dta/dta030.xml` | 조회/저장/삭제 SQL |
| Controller | `proj/dt/dta/dta030/Dta030Controller.java` | 엑셀업로드, 검증 |
| Service | `proj/dt/dta/dta030/Dta030Service.java` | 검증 로직, 데이터 가공 |

**연결 흐름**:
```
DTA030.html
  ├─ 조회: platform.post(platform.url.select, {statement: "dta030.selectDstbtPrdt"})
  │    → CommonController → CommonService → dta030.xml#selectDstbtPrdt
  ├─ 저장: platform.post(platform.url.saveAll, param.data)
  │    → CommonController → CommonService → dta030.xml#insertData
  ├─ 엑셀업로드: platform.post("/dta030/uploadExcel", param)
  │    → Dta030Controller.uploadRtDrct() → Dta030Service.validation()
  └─ 팝업: DataModalNew → /view → DTA030_P01.html
       └─ 팝업 내 프로시저: platform.post(platform.url.procedure, {statement: "dta030.dstbtPrdtAutoCrt"})
```

### 샘플 2: DTJ010 (제한매장설정) - 탭 기반 목록-상세

| 아티팩트 | 파일 | 역할 |
|----------|------|------|
| HTML (메인) | `templates/project/dt/dtj/DTJ010.html` | 탭 컨테이너 |
| HTML (탭1) | `templates/project/dt/dtj/DTJ010_T10.html` | 매장별 제한매장 |
| HTML (탭2) | `templates/project/dt/dtj/DTJ010_T20.html` | 일자별 제한매장 |
| HTML (팝업) | `templates/project/dt/dtj/DTJ010_P01.html` | 엑셀 업로드 팝업 |
| XML | `mapper/sjerp/dt/dtj/dtj010.xml` | 조회/저장 SQL |
| Controller | `proj/dt/dtj/dtj010/Dtj010Controller.java` | 엑셀 업로드 |
| Service | `proj/dt/dtj/dtj010/Dtj010Service.java` | 업로드 데이터 가공 |

**연결 흐름**:
```
DTJ010.html
  ├─ 탭 초기화: new DataTab(`${PGM}tab`, listener)
  ├─ 탭1 (DTJ010_T10.html):
  │    ├─ 조회: platform.post(platform.url.select, {statement: "dtj010.selectLmtShop"})
  │    ├─ 저장: platform.post("/dt/saveAll", param.data)
  │    └─ 행 클릭 → 우측 그리드 로드
  └─ 팝업 (DTJ010_P01.html):
       └─ 엑셀업로드: DataXlsModal → /dtj010/uploadRtLmt → Dtj010Controller → Dtj010Service
```

### 샘플 3: DTA020 (배분기준정보) - 좌우 분할 목록-상세

| 아티팩트 | 파일 | 역할 |
|----------|------|------|
| HTML | `templates/project/dt/dta/DTA020.html` | 좌우 분할 (좌: 공통기준, 우: 브랜드기준) |
| XML | `mapper/sjerp/dt/dta/dta020.xml` | 조회/upsert/삭제 SQL |
| Controller | 없음 (CommonController 사용) | - |
| Service | 없음 (CommonService 사용) | - |

**연결 흐름**:
```
DTA020.html
  ├─ 좌측 그리드 조회: platform.post(platform.url.select, {statement: "dta020.selectCommPrdtStdDtInfo"})
  ├─ 우측 그리드 조회: platform.post(platform.url.select, {statement: "dta020.selectBrndPrdtStdDtInfo"})
  ├─ 좌측 저장: platform.post(platform.url.saveAll, param.data)
  │    → Param.add("dta020.insertPrdtStdDtInfo", insertData)
  │    → Param.add("dta020.deletePrdtStdDtInfo", deleteData)
  └─ 모두 CommonController/CommonService 경유
```

**핵심 발견**: 전용 Controller/Service가 필요한 경우는 **엑셀 업로드, 파일 처리, 복잡한 서버 사이드 검증** 등에 한정.
단순 CRUD 화면은 CommonController만으로 동작.

---

## 5. 반복적으로 확인된 표준 패턴

### 5.1 Controller 표준 패턴

| 패턴 | 설명 | 반복 확인 근거 |
|------|------|---------------|
| 어노테이션 조합 | `@Slf4j @Controller @RequestMapping("/{module_id}")` | 모든 레퍼런스 Controller |
| 메서드 어노테이션 | `@AddUserInfo @ResponseBody @RequestMapping(POST)` | 모든 메서드 |
| 파라미터 수신 | `(HttpServletRequest request, @RequestBody BaseParm param)` | 모든 메서드 |
| LoginUserVo 추출 | `request.getAttribute(FrameConstants.LOGIN_USER_ATTR)` | 모든 Controller |
| 응답 반환 | `BaseResponse.Ok(result)`, `.Warn(msg)`, `.Error(msg)` | 모든 메서드 |
| 예외 처리 | FramePostgresException → CUSTOM_P9999 분기 | 모든 메서드 |
| 생성자 주입 | CommonService + 전용 Service 주입 | 모든 Controller |

### 5.2 Service 표준 패턴

| 패턴 | 설명 | 반복 확인 근거 |
|------|------|---------------|
| 클래스 구조 | `@Service @Slf4j extends BaseService` | 모든 Service |
| 트랜잭션 | `@Transactional(value = "txManager")` (쓰기), readOnly (읽기) | 모든 쓰기 메서드 |
| login 정보 주입 | 각 데이터 항목에 login_* 파라미터 세팅 | 모든 저장 메서드 |
| 벌크 최적화 | Set 수집 → 일괄 조회 → Map 변환 → O(1) 검색 | Dta030Service, Dtb020Service |

### 5.3 XML Mapper 표준 패턴

| 패턴 | 설명 | 반복 확인 근거 |
|------|------|---------------|
| namespace | 모듈ID 소문자 (1파일=1namespace) | 97개 XML 전수 |
| resultType | `java.util.Map` | 거의 모든 select |
| 동적 조건 | `<if test="P_xxx != null and P_xxx != ''">` | 모든 select |
| audit 컬럼 | 8개 감사 컬럼 (firs_reg_*, fina_reg_*) | 모든 insert/update |
| upsert | `ON CONFLICT (pk) DO UPDATE SET ... excluded.*` | dta020, dta050, dtb010, dtf010 |
| 코드명 변환 | `fn_sy_get_code_nm()`, `fn_ps_get_brndz_nm()` | 대부분의 select |
| comp_cd 필터 | `WHERE comp_cd = #{login_comp_cd}` | 모든 쿼리 |

### 5.4 HTML 표준 패턴

| 패턴 | 설명 | 반복 확인 근거 |
|------|------|---------------|
| fragment 선언 | `th:fragment="content(PGM, PARENT)"` | 모든 HTML |
| IIFE + listener | `(function(){ const PGM=...; const listener=platform.listener[PGM]; })()` | 모든 HTML |
| DOM ID prefix | `th:id="\|${PGM}xxx\|"` | 모든 HTML |
| 검색폼 | `webix.ui({view:'dataForm', search:true, ...})` | 검색이 있는 모든 화면 |
| 그리드 | `webix.ui({view:'datagrid', listener:listener, ...})` | 그리드가 있는 모든 화면 |
| 버튼 이벤트 | `listener.button.search.click = function(){}` | 모든 HTML |
| platform.post | `platform.post(platform.url.select, param, callback)` | 모든 서버 호출 |
| Callback | `new Callback(function(result){...})` | 모든 서버 호출 |

---

## 6. 예외 패턴 및 비표준 패턴

| 패턴 | 위치 | 비표준 이유 | 처리 방침 |
|------|------|-------------|-----------|
| DtController가 BaseController 미상속 | FERP DtController | GDI의 BaseController 상속이 관행이나, FERP에서는 생략 | 상속 권장하되 강제하지 않음 |
| 직접 URL 호출 | DTJ010_T10.html `platform.post("/dt/saveAll",...)` | platform.url.saveAll 미사용 | platform.url.* 사용 권장 |
| DELETE-then-INSERT | dta030.xml insertData | 일반적으로 upsert 사용이 표준 | 특수 케이스로 허용 (교체 시나리오) |
| REQUIRES_NEW 트랜잭션 | DtService.insertPair() | 부분 실패 허용이 목적 | 특수 케이스로 허용 |
| OPCPackage 직접 사용 | Dtj010Controller | ExcelUtil 대신 Apache POI 직접 사용 | 특수 엑셀 파싱 시 예외 허용 |

---

## 7. 지원할 화면 유형 정의

| 유형 | 설명 | 필수 산출물 | 선택 산출물 | 대표 샘플 |
|------|------|-------------|-------------|-----------|
| **list** | 조회+목록 중심 | HTML, XML | Controller, Service | DTA010 |
| **list-detail** | 목록+상세/편집 | HTML, XML, (Controller, Service) | 탭 HTML | DTA020, DTJ010 |
| **form** | 단건 입력/수정 | HTML, XML | Controller, Service | 근거 부족 (추가 분석 필요) |
| **popup** | 모달 팝업 | HTML | XML, Controller, Service | DTA030_P01, DT_DUPL_CONFIRM_P01 |

상세 정의: `system/templates/screen-types/*.yml`

---

## 8. 새 시스템에서 강제해야 할 규칙

### 8.1 구조 규칙
1. 모든 화면은 skeleton 기반으로 생성 (자유 생성 금지)
2. HTML은 `th:fragment="content(PGM, PARENT)"` 선언 필수
3. DOM ID는 `${PGM}` prefix 필수
4. 공통 fragment (`top_title_button`) 포함 필수
5. JS는 IIFE + `platform.listener[PGM]` 패턴 필수

### 8.2 서버 규칙
6. Controller는 `@AddUserInfo @ResponseBody @RequestMapping(POST)` 조합 필수
7. 응답은 `BaseResponse` 래퍼 필수
8. Service는 `@Transactional` 필수 (쓰기 작업)
9. login 정보는 `@AddUserInfo` AOP가 자동 주입

### 8.3 데이터 규칙
10. XML namespace는 모듈ID와 동일
11. 모든 WHERE에 `comp_cd = #{login_comp_cd}` 필수
12. INSERT 시 8개 audit 컬럼 필수
13. 파라미터 접두사: 검색은 `P_`, 로그인은 `login_`
14. `resultType="java.util.Map"` 사용

상세: `system/policies/framework/*.yml`

---

## 9. 금지해야 할 구현

| ID | 금지 항목 | 근거 |
|----|-----------|------|
| F-HTML-001 | 자유 HTML 구조 작성 | 프레임워크 data-page CSS 기반 동작 |
| F-HTML-002 | 순수 HTML form/table | webix.ui 사용이 표준 |
| F-HTML-003 | jQuery AJAX 직접 호출 | platform.post() 통합 사용 |
| F-HTML-004 | 전역 함수/변수 선언 | IIFE + listener 패턴 필수 |
| F-CTRL-003 | Controller에서 DB 직접 접근 | Service 경유 필수 |
| F-CTRL-004 | GET 메서드 사용 | POST 통일 |
| F-XML-001 | namespace 규칙 위반 | statement 참조 구조 깨짐 |
| F-XML-003 | audit 컬럼 누락 | 이력 관리 불가 |
| F-XML-004 | comp_cd 없는 WHERE | 멀티 회사 데이터 격리 위반 |
| F-GEN-001 | skeleton 미사용 직접 생성 | 일관성 보장 불가 |

상세: `system/policies/framework/forbidden_apis.yml`

---

## 10. 아직 미해결인 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| form 화면 유형 대표 샘플 | 근거 부족 | dt 모듈에 순수 form 독립 화면 미확인. sy 등 타 모듈 분석 필요 |
| build tool (Maven/Gradle) | 미확인 | pom.xml 또는 build.gradle 미분석 |
| DB 스키마 (public 외) | 미확인 | 실제 스키마 구조 확인 필요 |
| 권한 체크 상세 로직 | 미확인 | PgmPermission 클래스 상세 분석 필요 |
| 공통 코드 wrk_tp_cd 전체 목록 | 미확인 | DB 조회 필요 |
| DataTab, DataModalNew, Param 클래스 상세 API | 미확인 | webix.custom*.js 상세 분석 필요 |
| 엑셀 다운로드 패턴 | 미확인 | excel.worker.js 상세 분석 필요 |
| FERP reader/writer SqlSession 분리 vs GDI 단일 SqlSession | 확인 필요 | GDI에서는 단일 txManager, FERP에서는 reader/writer 분리 |
| trigger 사용 여부 | 미확인 | DB 직접 조회 필요 |

---

## 11. 다음 단계에서 해야 할 작업

1. **skeleton 파일 생성**: `skeleton_contract.yml` 기반으로 HTML/XML/Controller/Service skeleton 파일 작성
2. **form 유형 보완**: sy 모듈 등에서 순수 form 화면 샘플 확보
3. **analyze/build/fix command 구현**: skeleton + convention cache 기반 코드 생성 명령어 작성
4. **convention cache 적재**: SQL 초안으로 실제 DB 조회하여 cache 파일 채우기
5. **schema 파일 작성**: machine_spec, people_spec 등의 JSON Schema 정의
6. **webix 커스텀 API 상세 분석**: DataTab, DataModalNew, Param, Callback 등의 정확한 API 문서화
7. **GDI vs FERP 차이점 통합**: SqlSession 분리 여부 등 실제 GDI 환경에 맞는 조정

---

## 12. 산출물 목록

이번 단계에서 작성된 파일 목록:

| 파일 | 단계 | 설명 |
|------|------|------|
| `system/config/framework_manifest.yml` | 12단계 | 프레임워크 종합 매니페스트 |
| `system/config/naming.yml` | 3단계 | 네이밍 규칙 정의 |
| `system/policies/framework/controller_patterns.yml` | 4단계 | Controller 패턴 정책 |
| `system/policies/framework/service_patterns.yml` | 5단계 | Service 패턴 정책 |
| `system/policies/framework/xml_patterns.yml` | 6단계 | XML Mapper 패턴 정책 |
| `system/policies/framework/html_patterns.yml` | 7단계 | HTML 템플릿 패턴 정책 |
| `system/policies/framework/layout_rules.yml` | 7단계 | 레이아웃 규칙 정책 |
| `system/policies/framework/template_selection.yml` | 9단계 | 화면 유형별 skeleton 선택 규칙 |
| `system/policies/framework/skeleton_contract.yml` | 10단계 | Skeleton placeholder 계약 |
| `system/policies/framework/forbidden_apis.yml` | 11단계 | 금지 패턴 정책 |
| `system/templates/screen-types/list.yml` | 8단계 | list 화면 유형 정의 |
| `system/templates/screen-types/list-detail.yml` | 8단계 | list-detail 화면 유형 정의 |
| `system/templates/screen-types/form.yml` | 8단계 | form 화면 유형 정의 |
| `system/templates/screen-types/popup.yml` | 8단계 | popup 화면 유형 정의 |
| `system/cache/convention/sql/code_select.sql` | 13단계 | 공통코드 캐시 수집 SQL |
| `system/cache/convention/sql/table_select.sql` | 13단계 | 테이블 정의 캐시 수집 SQL |
| `system/cache/convention/sql/view_select.sql` | 13단계 | 뷰 정의 캐시 수집 SQL |
| `system/cache/convention/sql/function_select.sql` | 13단계 | 함수 정의 캐시 수집 SQL |
| `system/cache/convention/sql/trigger_select.sql` | 13단계 | 트리거 정의 캐시 수집 SQL |
| `framework_analysis_report.md` | 14단계 | 본 보고서 |
