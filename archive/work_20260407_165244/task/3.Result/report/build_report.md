# Build Report — 사전검토관리 (SEB010/SEB011/SEB012)

## 1. 빌드 요약

| 항목 | 값 |
|------|-----|
| 소스 PPT | GDI_DE_331_화면설계서_통합본.pptx |
| 추출 화면 수 | 3 |
| 생성 산출물 | HTML 3, XML 3, SQL 1 |
| 검증 결과 | PASS (경고 3건) |
| 커스텀 Controller/Service | 불필요 (CommonController 사용) |

## 2. 화면별 생성 결과

### SEB010 — 사전검토관리 (list-detail)
- **HTML**: 검색폼 2행 + 좌측 마스터그리드 + 우측 검토대상자그리드
- **XML**: selectMainGrid (JOIN rp_rsch_prj_mst), selectDetailGrid, deletePreRviewMst
- **특징**: 좌우 분할, 마스터 행 클릭 시 우측 그리드 갱신, 더블클릭 시 SEB011 이동

### SEB011 — 사전검토등록 (form)
- **HTML**: 폼(연구과제명 커스텀코드헬프 + 검토기간) + 편집그리드(검토대상자)
- **XML**: selectInfo, selectDetailGrid, selectRschPrjList, insertPreRviewMst, insertPreRviewOplList, deletePreRviewMst
- **특징**: activePgm 패턴, 커스텀 코드헬프 모달, saveAll 다중 테이블 저장

### SEB012 — 사전검토의견서등록내용 (form)
- **HTML**: readonly 연구과제내용 + editable 의견서등록 (채택여부 조건부 제어)
- **XML**: selectInfo, insertPreRviewOplList
- **특징**: 채택여부 변경 시 불채택사유 readonly 토글

## 3. Review Diff (original → final)

| 항목 | 변경 내용 |
|------|----------|
| 접수일자 | DATE → DATE_RANGE, 기본값 ±7일 |
| 단독/공동 | 코드타입 확정 → RP370 |
| 내부/외부 | 코드타입 확정 → RP380 |
| SEB012 호출 | SEB010에서 직접 |
| 검토대상자 | 행 클릭 → 우측 그리드 갱신 확인 |

## 4. 경고 사항

1. **W-001**: SEB011 saveAll 트랜잭션 — wrk_mast_id 생성 순서 검토 필요
2. **W-002**: 메뉴등록 SQL — uppr_menu_id TODO (SE/SEB 상위 메뉴 ID 확인 필요)
3. **W-003**: SEB011 알림 버튼 — API 연동 미정으로 구현 보류

## 5. Deploy 대상 경로

| 파일 | Target Path |
|------|------------|
| SEB010.html | templates/project/se/seb/SEB010.html |
| seb010.xml | mapper/sjerp/se/seb/seb010.xml |
| SEB011.html | templates/project/se/seb/SEB011.html |
| seb011.xml | mapper/sjerp/se/seb/seb011.xml |
| SEB012.html | templates/project/se/seb/SEB012.html |
| seb012.xml | mapper/sjerp/se/seb/seb012.xml |
| SEB010_메뉴등록.sql | DB 직접 실행 |
