# Skeleton 추출 보고서

## 1. 작업 개요

본 작업은 이전 단계 프레임워크 분석 결과를 바탕으로, 새 시스템이 사용할 html/xml/controller/service skeleton 템플릿을 추출·정규화한 것이다.

- **작업 일자**: 2026-04-05
- **작업 단계**: Skeleton 추출 및 템플릿 정규화 (명령어 구현 전 단계)
- **Placeholder 표기**: `{{placeholder_name}}` 형식으로 통일
- **OPTIONAL 블록 표기**: `/* OPTIONAL: block name */` 또는 `<!-- OPTIONAL: block name -->` 주석

---

## 2. 입력으로 사용한 이전 단계 결과물

| 파일 | 용도 |
|------|------|
| `system/config/framework_manifest.yml` | 프레임워크 기술 스택, 기반 클래스, 공통 엔드포인트 확인 |
| `system/config/naming.yml` | 네이밍 규칙 → skeleton 내 클래스명/파일명/URL 패턴 반영 |
| `system/policies/framework/controller_patterns.yml` | Controller 어노테이션, 메서드 구조, 예외 처리 패턴 |
| `system/policies/framework/service_patterns.yml` | Service 클래스 구조, 트랜잭션, mapper 호출 패턴 |
| `system/policies/framework/xml_patterns.yml` | namespace, SQL ID, 동적 SQL, audit 컬럼 패턴 |
| `system/policies/framework/html_patterns.yml` | Thymeleaf fragment, webix.ui, listener 패턴 |
| `system/policies/framework/layout_rules.yml` | 화면 레이아웃 구조, CSS 클래스 |
| `system/policies/framework/forbidden_apis.yml` | 금지 패턴 → skeleton이 위반하지 않는지 검증 |
| `system/templates/screen-types/*.yml` | 화면 유형별 정의, 필수/선택 영역 |
| `framework_analysis_report.md` | 분석 개요 및 대표 샘플 연결 구조 |

---

## 3. 대표 샘플 선정 결과

### list 유형
| 항목 | 값 |
|------|-----|
| HTML | `C:/GitLap/FERP/.../templates/project/dt/dta/DTA010.html` |
| XML | `C:/GitLap/FERP/.../mapper/sjerp/dt/dta/dta010.xml` |
| Controller | 없음 (CommonController 사용) |
| Service | 없음 (CommonService 사용) |
| 선택 이유 | 가장 단순한 검색+목록+그리드 편집 구조. 전용 Controller 없이 동작하는 전형적 list 화면 |
| 공통부 | fragment 선언, IIFE, listener 구조, searchForm, mainGrid, button 이벤트, platform.post |
| 업무특화부 | 검색 필드(브랜드존/스타일 등), 그리드 컬럼(가격/수량 등), SQL statement 명 |

### list-detail 유형
| 항목 | 값 |
|------|-----|
| HTML | `C:/GitLap/FERP/.../templates/project/dt/dta/DTA020.html` |
| XML | `C:/GitLap/FERP/.../mapper/sjerp/dt/dta/dta020.xml` |
| Controller | 없음 (CommonController 사용) |
| Service | 없음 (CommonService 사용) |
| 선택 이유 | 좌우 분할 레이아웃(data-content-5-left + data-content-fill)의 전형적 마스터-디테일 구조 |
| 공통부 | 좌우 분할 구조, 마스터 행 클릭 → 상세 로드, 개별 저장 이벤트, Param.add() 패턴 |
| 업무특화부 | 공통기준/브랜드기준 컬럼, 년차 검증 로직, 삭제 버튼 커스텀 렌더링 |

### form 유형
| 항목 | 값 |
|------|-----|
| HTML | 근거 부족 (dt 모듈에서 순수 form 독립 화면 미확인) |
| 참고 | DTA030_P01.html (팝업 폼)의 폼 구조를 form 유형으로 확장 |
| 선택 이유 | DTA030_P01의 dataForm 패턴이 form 화면의 핵심 구조를 포함 |
| 공통부 | dataForm 정의, 폼 검증, 저장 이벤트, platform.post |
| 업무특화부 | 폼 필드, 저장 statement |

### popup 유형
| 항목 | 값 |
|------|-----|
| HTML (입력형) | `C:/GitLap/FERP/.../templates/project/dt/dta/DTA030_P01.html` |
| HTML (확인형) | `C:/GitLap/FERP/.../templates/project/dt/DT_DUPL_CONFIRM_P01.html` |
| HTML (업로드형) | `C:/GitLap/FERP/.../templates/project/dt/dtj/DTJ010_P01.html` |
| 선택 이유 | 3가지 서브타입(입력/확인/업로드) 모두 확보하여 VARIANT 블록 설계 |
| 공통부 | listener.modal, listener.modalParam, dataModal.ok() 반환 패턴 |
| 업무특화부 | 폼 필드, 그리드 컬럼, 서버 호출 여부 |

---

## 4. 화면유형별 skeleton 추출 근거

### 공통 추출 원칙
- **고정 블록**: 모든 화면에서 동일하게 반복되는 구조 (fragment, IIFE, listener 초기화, 예외 처리)
- **치환 블록**: `{{placeholder}}` 로 대체되는 업무 특화 부분 (필드, 컬럼, SQL, 클래스명)
- **선택 블록**: `/* OPTIONAL */` 주석으로 표시, 조건에 따라 활성/비활성
- **변형 블록**: `/* VARIANT */` 주석으로 표시, 서브타입에 따라 선택 (popup에서 사용)

---

## 5. HTML Skeleton 설계 요약

| 파일 | 구조 | 핵심 구성 요소 |
|------|------|---------------|
| `list.html` | searchForm + mainGrid | 검색폼, 그리드, 조회/저장/초기화 버튼 |
| `list-detail.html` | searchForm + masterGrid + detailGrid | 좌우 분할, 마스터 행 클릭 → 상세 로드, 개별 저장 |
| `form.html` | searchForm(선택) + mainForm + subGrid(선택) | 입력 폼, 단건 조회/저장, 하위 목록(선택) |
| `popup.html` | postForm(입력형) / popupGrid(조회형) | modal/modalParam 접근, dataModal.ok() 반환 |

**공통 구조** (모든 HTML skeleton):
```
th:fragment="content(PGM, PARENT)"
<th:block th:replace="~{frame/top_title_button :: topTitleButtonFragment}">
(function() { const PGM = ...; const listener = platform.listener[PGM]; ... })();
```

---

## 6. XML Skeleton 설계 요약

| 파일 | SQL 구성 | 핵심 패턴 |
|------|----------|-----------|
| `list.xml` | select + upsert(선택) + delete(선택) | 단일 테이블, ON CONFLICT 지원 |
| `list-detail.xml` | master select + detail select + upsert + delete | 이중 테이블, 마스터/상세 분리 |
| `form.xml` | select(선택) + upsert + delete(선택) | 단건 조회/저장 |
| `popup.xml` | select(선택) + insert(선택) + procedure(선택) | 최소 구조, 모든 SQL 선택적 |

**공통 구조** (모든 XML skeleton):
```xml
<!DOCTYPE mapper ...>
<mapper namespace="{{mapper_namespace}}">
  <!-- audit 컬럼 8개 필수 -->
  <!-- comp_cd = #{login_comp_cd} 필수 -->
  <!-- ON CONFLICT ... DO UPDATE SET 패턴 -->
```

---

## 7. Controller Skeleton 설계 요약

| 파일 | 메서드 구성 | 핵심 패턴 |
|------|------------|-----------|
| `list.java` | uploadExcel(선택) + validation(선택) | 엑셀 업로드/검증 특화 |
| `list-detail.java` | saveData(선택) + deleteData(선택) | 복잡 트랜잭션 저장/삭제 |
| `form.java` | saveData | 단건 저장 |
| `popup.java` | process(선택) | 팝업 내 서버 처리 |

**공통 구조** (모든 Controller skeleton):
```java
@Slf4j @Controller @RequestMapping("/{{request_base_path}}")
// 생성자 주입: CommonService + 전용 Service
// 메서드: @AddUserInfo @ResponseBody @RequestMapping(POST)
// 예외: FramePostgresException → CUSTOM_P9999 분기
// 반환: BaseResponse.Ok() / .Warn() / .Error()
```

---

## 8. Service Skeleton 설계 요약

| 파일 | 메서드 구성 | 핵심 패턴 |
|------|------------|-----------|
| `list.java` | uploadExcel(선택) + validation(선택) | 벌크 코드 검증 최적화 |
| `list-detail.java` | saveData(선택) + deleteData(선택) | 리스트 순회 insert/delete |
| `form.java` | saveData | 단건 insert |
| `popup.java` | process(선택) | 팝업 비즈니스 로직 |

**공통 구조** (모든 Service skeleton):
```java
@Service @Slf4j extends BaseService
// 생성자 주입: SqlSessionTemplate
// 저장 메서드: @Transactional(value = "txManager")
// login 정보 주입: login_comp_cd, login_emp_no, login_user_id, login_user_ip
```

---

## 9. Placeholder 계약 요약

### 표기 형식: `{{name}}`

| 분류 | Placeholder | 타입 | 필수 여부 |
|------|------------|------|-----------|
| 공통 | screen_id | string | 항상 |
| 공통 | module_id | string | 항상 |
| 공통 | module_group | string | controller/service |
| 공통 | sub_group | string | controller/service |
| 공통 | mapper_namespace | string | 항상 |
| HTML | search_fields | array_block | list/list-detail |
| HTML | grid_columns | array_block | list/list-detail |
| HTML | detail_fields | array_block | list-detail/form/popup |
| HTML | init_script | string_block | 선택 |
| XML | select_sql_id | string | 항상 |
| XML | insert_sql_id | string | 저장 있을 때 |
| XML | delete_sql_id | string | 삭제 있을 때 |
| XML | main_table | string | 항상 |
| XML | pk_columns | string | upsert 시 |
| Java | controller_class | string | controller |
| Java | service_class | string | controller/service |
| Java | request_base_path | string | controller |

상세: `system/policies/framework/skeleton_contract.yml`

---

## 10. Template Selection 규칙 요약

| 화면 유형 | 필수 Skeleton | 선택 Skeleton | Controller 필요 조건 |
|-----------|--------------|--------------|---------------------|
| list | html, xml | controller, service | 엑셀업로드/검증/파일 |
| list-detail | html, xml | controller, service | 복잡 트랜잭션/검증 |
| form | html, xml | controller, service | 파일/다중테이블/검증 |
| popup | html | xml, controller, service | 서버 처리 필요 시 |

**구분 규칙**:
- list vs list-detail: 상세 편집 영역 별도 존재 여부
- form vs popup: 독립 화면 vs DataModalNew 호출
- 자유 생성 금지, skeleton 미존재 시 생성 실패

---

## 11. 정합성 검증 결과

| 검증 항목 | 결과 | 비고 |
|-----------|------|------|
| screen-type yml ↔ skeleton 파일 일치 | OK | 4개 유형 × 4개 아티팩트 = 16개 skeleton 파일 |
| template_selection ↔ 실제 파일명 일치 | OK | skeleton 경로가 정확히 일치 |
| skeleton_contract 필수 placeholder ↔ skeleton 내 존재 | OK | 모든 필수 placeholder가 skeleton에 존재 |
| forbidden_apis ↔ skeleton 충돌 | OK | 금지 패턴 미포함 확인 |
| framework_manifest ↔ skeleton 모순 | OK | 기술 스택, 기반 클래스 일치 |
| html ↔ xml ↔ controller ↔ service 간 연결 | OK | mapper_namespace, sql_id, service_class 연결 일관 |

**보정 사항**:
- template_selection.yml: skeleton 경로를 `skeletons/controller/module_controller.java` → `system/templates/skeletons/controller/{type}.java`로 보정
- skeleton_contract.yml: 추상 정의 → 실제 skeleton 입력 계약서로 전면 보정
- screen-types/*.yml: 분석 설명서 → skeleton 선택 기준서로 전면 보정

---

## 12. 아직 미해결인 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| form 유형 대표 레퍼런스 | 근거 부족 | dt 모듈에 순수 form 독립 화면 없음. 팝업 폼을 확장하여 skeleton 작성 |
| tabbed 화면 skeleton | 미작성 | 탭 컨테이너 skeleton은 별도 작성 필요 (DTJ010.html 기반) |
| 엑셀 다운로드 패턴 | 미반영 | excel.worker.js 상세 분석 후 skeleton OPTIONAL 블록 추가 필요 |
| DataTab/DataModalNew API 상세 | 미확인 | webix.custom*.js 분석 필요 |
| GDI vs FERP SqlSession 분리 | 미확정 | skeleton은 GDI 패턴(단일 txManager) 기반으로 작성 |
| 배열형 placeholder 치환 엔진 | 미구현 | search_fields, grid_columns 등 배열형 placeholder를 실제 코드 블록으로 치환하는 로직 필요 |

---

## 13. 다음 단계에서 해야 할 작업

1. **analyze command 구현**: people_spec → machine_spec 변환, skeleton 선택 자동화
2. **build command 구현**: machine_spec + skeleton → 실제 코드 생성
3. **placeholder 치환 엔진**: 배열형 placeholder를 실제 webix.ui / SQL 코드로 변환하는 로직
4. **tabbed 화면 skeleton**: 탭 컨테이너 + 탭 콘텐츠 조합 skeleton 추가
5. **manifest schema 작성**: analyze_manifest, build_manifest JSON Schema
6. **convention cache 적재**: DB에서 실제 코드/테이블/뷰 정보 수집
7. **form 유형 레퍼런스 보완**: sy 모듈 등에서 순수 form 화면 확보

---

## 14. 작성/수정 파일 목록

### 신규 생성 (16건 - skeleton)
| 파일 | 유형 |
|------|------|
| `system/templates/skeletons/html/list.html` | HTML skeleton |
| `system/templates/skeletons/html/list-detail.html` | HTML skeleton |
| `system/templates/skeletons/html/form.html` | HTML skeleton |
| `system/templates/skeletons/html/popup.html` | HTML skeleton |
| `system/templates/skeletons/xml/list.xml` | XML skeleton |
| `system/templates/skeletons/xml/list-detail.xml` | XML skeleton |
| `system/templates/skeletons/xml/form.xml` | XML skeleton |
| `system/templates/skeletons/xml/popup.xml` | XML skeleton |
| `system/templates/skeletons/controller/list.java` | Controller skeleton |
| `system/templates/skeletons/controller/list-detail.java` | Controller skeleton |
| `system/templates/skeletons/controller/form.java` | Controller skeleton |
| `system/templates/skeletons/controller/popup.java` | Controller skeleton |
| `system/templates/skeletons/service/list.java` | Service skeleton |
| `system/templates/skeletons/service/list-detail.java` | Service skeleton |
| `system/templates/skeletons/service/form.java` | Service skeleton |
| `system/templates/skeletons/service/popup.java` | Service skeleton |

### 보정 (6건 - 정책/유형 정의)
| 파일 | 변경 내용 |
|------|-----------|
| `system/policies/framework/template_selection.yml` | skeleton 경로 연결, 구분 규칙 추가, 실패 처리 규칙 추가 |
| `system/policies/framework/skeleton_contract.yml` | 추상 정의 → 실제 입력 계약서 전면 보정 |
| `system/templates/screen-types/list.yml` | skeleton 선택 기준서로 보정, skeleton 경로 추가 |
| `system/templates/screen-types/list-detail.yml` | skeleton 선택 기준서로 보정, skeleton 경로 추가 |
| `system/templates/screen-types/form.yml` | skeleton 선택 기준서로 보정, 근거 부족 명시 |
| `system/templates/screen-types/popup.yml` | skeleton 선택 기준서로 보정, 서브타입 정리 |

### 신규 생성 (1건 - 보고서)
| 파일 | 설명 |
|------|------|
| `skeleton_extraction_report.md` | 본 보고서 |
