[신규 오류]
1. SEA010PPT에 있는 다음 지시사항을 수행하지 않았다. 트리거를 등록하는 sql이 뽑아져야 한다.
[데이터]
사전검토의견서마스터에 아래 내용의 트리거 생성 (SQL 생성)
사전검토의견서마스터의 채택결과 등록 시. 해당 사전검토가 사전검토완료 상태가 아닌 경우 사전검토에 물린 의견서를 확인하고 채택결과가 모두 등록되었다면 해당 일자를 사전검토마스터의 의견서작성일에 넣어준다. 그리고 사전검토마스터의 진행상태를 사전검토완료으로 바꾼다. 

















[아래는 해결됨 기록용]
1. 메뉴등록SQL 작성시 실제 DDL에 맞지 않는 쿼리를 제공함
- menu_mdul_id 컬럼 누락

2. build 수행 후 자동 deploy 되지 않음

3. 메뉴등록시 아래와 같이 쿼리를 작성해야한다.
잘못됨 : sy_pgm_info > pgm_path : '/project/se/seb/SEB010'
잘됨 : sy_pgm_info > pgm_path : '/se/seb/seb010'

4. HTML 생성 오류
  1) th:fragment="content(PGM, PARENT)" 제거 — 다른 화면(SEA010 등)과 동일하게 <html xmlns:th="http://www.thymeleaf.org">로 변경
  2) 존재하지 않는 ${PGM}btnToolbar 컨테이너를 사용하는 커스텀 webix 툴바 제거 — 이것이 _settings null 에러의 직접 원인이었습니다. webix.ui()가 DOM에 없는 컨테이너를 참조하면서 null 에러 발생

  프레임(top_title_button.html)이 이미 조회/신규/삭제/초기화 버튼을 제공하고 있고, listener.button.search.click, listener.button.news.click 등의 핸들러가 이미 올바르게 정의되어 있으므로 커스텀 툴바 없이도 정상 동작합니다.

5. APP_DATE.addDay 가 동작하지 않는다. 어디서 나온 코드인지 해명하고 수정해라.
{view: DATA_VIEW.dateRange2, label: "접수일자", name: "P_rcpt_dt", defaultValue: {start: APP_DATE.addDay(-7), end: APP_DATE.addDay(7)}

6. sy_code_dtl을 사용해야하는데 sy_comm_cd이란 없는 테이블을 사용 근본적으로 cahce데이터와 없다면 db 풀백을 해야하는데 그 절차가 근본적으로 동작하지 않는 것 같다. 이유를 이야기 하고 근본적으로 수정해라. 그리고 cahce데이터, 실제 DB에 해당 테이블, 코드 등등 정보가 없는데 절차상 있어야 한다면 해당 db 요소 생성 SQL을 반드시 제공해야한다.
1) 테이블 없음 sy_comm_cd > sy_code_dtl
2) 컬럼 없음 rp_pre_rview_mst > rcpt_dt 컬럼 없음

7. openPgm 오류 isNew 비교 방식
  openPgm의 시그니처가 openPgm(module_id, pgm_id, param)인데 SEB010에서는 인자 2개만 넘기고 있어서 pgm_id에 {isNew: true} 객체가 들어가고 param은 undefined가 됩니다.

  SEA010의 호출 방식과 비교하면:
  // SEA010 (정상)
  APP_PGM.openPgm('SE', 'SEA011', { isNew: true });

  // SEB010 (잘못됨)
  APP_PGM.openPgm('SEB011', {isNew: true});

  module_id 파라미터가 빠져 있습니다. 이것이 근본 원인입니다. openPgm은 (module_id, pgm_id, param) 3개 인자를 받는데, SEB010에서 모듈 ID('SE') 없이 2개만 넘기고 있습니다.


param && param.wrk_mast_id && String(param.isNew) !== 'true'


8. SEA010 그리드 레이아웃이 PPT로 보았을 때 마스터가 data-content-fill 디테일이 data-content-3-left 정도 여야하는데 안맞다.
9. SEA011 폼에서 [{},{},{},{},{}] 5칸 정책이 적용되지 않았다.

10. resetState() (라인 284)에서 listener.form.postForm.formReset() 등의 UI 조작을 하기 전에, initPgm이 아직 완료되지 않은 상태에서 activePgm이 호출되는 타이밍 문제가 아닙니다.

  실제 문제는 라인 284의 resetState() 호출입니다. resetState 함수(라인 23~26)는 단순히 변수만 초기화하지만, 에러 스택에서 resetState @ VM2296:14가 찍히고 있으므로 현재 실행 중인 코드와 파일 내용이 다를 수 있습니다.

  하지만 현재 코드 기준으로 보면, 진짜 문제는 activePgm이 최초 진입 시(탭을 새로 열 때) initPgm → activePgm 순서로 호출되는데, initPgm(라인 271~276)에서 이미 resetState()를 호출하고 formReadonly(true)로 설정한 직후, activePgm(라인  
  278)의 신규 모드 분기에서 다시 resetState()를 호출하면서 문제가 없어야 합니다.

  그런데 initPgm 내부에서 createPostForm()이 호출되고, 이 안에서 webix.ui()가 비동기적으로 초기화될 수 있습니다. activePgm이 호출되는 시점에 listener.form.postForm이 아직 준비되지 않았을 가능성이 있습니다.

  하지만 스택 트레이스를 다시 보면, _activePgmAsync가 loading.isLoading() 체크를 하고 있어 동기화가 되어야 합니다.

  실제 에러 메시지가 빠져있는데, 가장 유력한 원인은 라인 272에서 resetState()를 호출하는데, 이 시점에 listener.form.postForm이 아직 생성 전이라는 것입니다. 현재 resetState에는 formReset 호출이 없지만, 만약 이전 버전의 코드에서      
  resetState 안에 listener.form.postForm.init() 같은 호출이 있었다면 에러가 발생합니다.

  현재 코드의 initPgm 순서를 보면:

  listener.initPgm = () => {
      resetState();           // ← 라인 272: 여기서는 postForm이 아직 없음
      createPostForm();       // ← 라인 273: 여기서 postForm 생성
      createToolbars();
  };

  resetState 자체는 변수만 초기화하므로 문제 없지만, 에러는 activePgm에서 발생합니다. 스택 트레이스상 activePgm → resetState인데, activePgm의 신규 모드 분기(라인 284~289)에서 resetState() 후 listener.form.postForm.formReset()을     
  호출합니다. 이 시점에 postForm이 존재하므로 문제 없어야 합니다.

  결론적으로, 에러가 발생하는 실제 원인은 initPgm의 resetState() 호출(라인 272)이 아니라, 프레임워크의 activePgm 호출 시점입니다.
  
11. 코드 헬프 SQL 존재 위치

원인이 명확합니다.

  프레임워크의 코드 헬프 컴포넌트(webix.custom3.js)가 statement 검증 시 code., codeCustom., codePager.로 시작하는 namespace만 허용합니다:

  if(!this._codeOption.statement.startsWith("code.")
     && !this._codeOption.statement.startsWith("codeCustom.")
     && !this._codeOption.statement.startsWith("codePager.")){
      webix.message({type:"error", text:`유효하지 않은 statement : ${this._codeOption.statement}`});
  }

  SEB011에서는 statement: "seb011.selectRschPrjList"로 설정했는데, seb011.은 허용된 prefix가 아니므로 "유효하지 않은 statement" 에러가 발생합니다.

  해결 방법: selectRschPrjList 쿼리를 codeCustom namespace의 mapper로 이동하거나, statement를 codeCustom.selectRschPrjList로 변경하고 해당 mapper에 쿼리를 등록해야 합니다.


12. 특별한 지시 없으면 코드헬프 컴포넌트는 아래 속성으로 세팅
- view : DATA_VIEW.code 대신 DATA_VIEW.codeNone
- type : DATA_CODE_TYPE.code 대신 DATA_CODE_TYPE.codeValue

13. SEB011 의 폼의 컴포넌트 레이아웃이 한줄에 5칸 규칙이 적용되지 않았다. ppt 상으로 보면 폼 한줄에 컴포넌트가 2개 있고 너비는 연구과제명 2칸, 검토기간 1칸으로 작성되어 있다.
그런데 아래와 같이 elements: [{cols:[{}]},{cols:[{}]}] 으로 되어 있다. PPT 상으로 그리고 정책상으로 [{cols:[{연구과제검색 gravity 2},{검토기간 gravity 1},{},{}]}] 이렇게 되는게 맞다. 폼 뿐만 아니라 조회 영역도 마찬가지이다. 근본적으로 수정해라

        listener.form.mainForm = webix.ui({
            container: `${PGM}mainForm`,
            view: 'dataForm',
            pgm: PGM,
            elements: [
                {
                    cols: [
                        {view: DATA_VIEW.code, type: DATA_CODE_TYPE.custom, label: "연구과제명", name: "rsch_prj_id", required: true,
                            statement: "codeCustom.selectRschPrjList",
                            displayName: "prj_nm",
                            modalConfig: {
                                title: "연구과제 검색",
                                width: 700,
                                height: 500,
                                searchFields: [
                                    {view: DATA_VIEW.codeNone, type: DATA_CODE_TYPE.codeValue, label: "대상년도", name: "P_prj_targ_yy", param: {wrk_tp_cd: 'RP', cd_tp_cd: '350'}},
                                    {view: DATA_VIEW.codeNone, type: DATA_CODE_TYPE.codeValue, label: "연구분류", name: "P_rsch_ctg_cd", param: {wrk_tp_cd: 'RP', cd_tp_cd: '010'}},
                                    {view: DATA_VIEW.codeNone, type: DATA_CODE_TYPE.codeValue, label: "연구유형", name: "P_rsch_cls_cd", param: {wrk_tp_cd: 'RP', cd_tp_cd: '020'}},
                                    {view: DATA_VIEW.text, label: "연구과제명", name: "P_prj_nm"}
                                ],
                                gridColumns: [
                                    {id: "rsch_prj_id", header: "과제ID", width: 100},
                                    {id: "prj_nm", header: "연구과제명", fillspace: true},
                                    {id: "rsch_cls_nm", header: "연구유형", width: 100},
                                    {id: "prj_targ_yy", header: "대상년도", width: 80, css: "textCenter"}
                                ],
                                returnFields: {rsch_prj_id: "rsch_prj_id", prj_nm: "prj_nm"}
                            }
                        }
                    ]
                },
                {
                    cols: [
                        {view: DATA_VIEW.dateRange2, label: "검토기간", startName: "rview_tmlt_start_dt", endName: "rview_tmlt_end_dt", required: true}
                    ]
                }
            ]
        });

14. DATA_CODE_TYPE.custom도 마찬가지이다. DATA_VIEW.code 뒤에 타입과 관계없이 DATA_VIEW.code > DATA_VIEW.codeNone 가 되도록 설정해라
DATA_VIEW.code, type: DATA_CODE_TYPE.custom
DATA_VIEW.codeNone, type: DATA_CODE_TYPE.custom


15. PPT에서 SEA010 그리드 위에 툴바가 있는데 코드에는 존재하지 않는다. 조회 영역을 제외하고 폼 영역, 그리드 영역 위에는 반드시 toolbar가 위치해서 해당 섹션이 뭔지 title이 박혀야 한다. PPT에 없다면 '작성필요'라고 빈 툴바를 입력해 놓도록 수정하라

            listener.toolbar.toolbar1 = webix.ui({
                container: `${PGM}toolbar1`,
                view: 'dataToolbar',
                pgm: PGM,
                title: '사전검토목록',
            });

16. Listener setParent error Listener is null

  원인: SEB011.html 라인 94에서 listener.setParent(PARENT) 호출 시, PARENT 변수가 null입니다.

  setParent는 platform.listener[parent]가 존재하는지 확인하는데(ui.js:290), PARENT가 null이면 platform.listener[null]은 undefined이므로 에러가 발생합니다.

  SEB010에서 APP_PGM.openPgm으로 SEB011을 열 때, 프레임워크가 Thymeleaf에 PARENT 값을 전달해야 하는데, SEB011이 독립 탭으로 로드되면서 PARENT가 설정되지 않습니다. 이 프로젝트의 openPgm은 hash 기반 라우팅이라 부모-자식 관계를 서버에 
  전달하지 않습니다.

  setParent는 부모의 auth(권한)를 상속받기 위한 것인데, SEB011이 top_title_button 프레임을 직접 include하고 있어 자체적으로 auth를 받으므로 setParent 호출이 불필요합니다.

3. 유효하지 않은 코드옵션입니다

  원인: DATA_CODE.custom의 기본 정의(code.js:123~132)에 idField와 valueField가 없습니다:

  custom : {
      apiUrl : "/common/select",
      viewPath: "/help/codeHelp",
      title : '검색',
      columns : [],
      requireField : [],
      searchElement : [...],
      // idField 없음!
      // valueField 없음!
      // statement 없음!
  }

  프레임워크 검증(webix.custom3.js:1268~1270):
  if(!this._codeOption.statement || !this._codeOption.apiUrl || !this._codeOption.viewPath ||
     !this._codeOption.idField || !this._codeOption.valueField || !this._codeOption.requireField)

  SEB011에서 statement는 config에서 넘기고 있지만, idField와 valueField를 지정하지 않았고 custom 기본값에도 없으므로 검증 실패합니다.

  다른 화면의 custom 사용 예시(prnt.js)를 보면 idField, valueField를 명시합니다. SEB011의 코드 헬프 설정에 이 두 필드를 추가해야 합니다.

      58                      cols: [
      59                          {view: DATA_VIEW.codeNone, type: DATA_CODE_TYPE.custom, label: "연구과제명", name: "rsch_prj_id", required: true, gravity: 2,
      60                              statement: "codeCustom.selectRschPrjList",
      61 +                            idField: "rsch_prj_id",                                                                                                                                                                           
      62 +                            valueField: "prj_nm",                                                                                                                                                                             
      63                              displayName: "prj_nm",
      64                              modalConfig: {
      65                                  title: "연구과제 검색",
