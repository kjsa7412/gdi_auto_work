# Cache + DB Fallback 실행 체인 통합 보고서

## 1. 작업 개요
convention cache와 DB fallback 절차를 analyze/build/fix 실행 체인에 명시적으로 연결하였다. cache-first 원칙 하에서, cache에 정보가 없거나 부족할 때 실제 DB SELECT 조회로 보완하는 절차를 command, policy, schema, manifest 전체에 일관되게 반영하였다.

## 2. 현재 문제 인식 (수정 전)
- analyze에 cache refresh는 있으나, refresh 후 부족 시 DB fallback 절차 미명시
- build에 "DB fallback 안 함"으로만 되어 있어, cache miss 시 검증 불가 상태
- fix에 "db_access.yml에 따라 판정"으로만 되어 있어, 실제 절차 불명확
- manifest에 hit/miss 구분, fallback reason/queries/status 기록 필드 없음

## 3. Cache와 DB Fallback 역할 재정의

| 용어 | 정의 |
|------|------|
| **cache hit** | 필요한 정보가 cache에서 충분히 확인된 상태 |
| **cache miss** | 필요한 항목이 cache에 전혀 존재하지 않는 상태 |
| **cache insufficient** | 일부 존재하지만 판단에 필요한 수준으로 충분하지 않은 상태 |
| **cache mismatch** | cache 정보와 분석/생성 대상 구조가 충돌하는 상태 |
| **db fallback** | cache miss/insufficient/mismatch를 해소하기 위한 DB SELECT 조회 |

반영 위치: `system/config/runtime.yml` → `cache.terminology`

## 4. Analyze 체인 보강

**수정 파일**: `.claude/commands/analyze.md`

| 항목 | 내용 |
|------|------|
| 2단계 | Convention Cache Refresh (필수, 무조건 1회) — psql 명령 명시 |
| 2-1단계 | **신규** — Cache 기반 정보 보강 및 DB Fallback |
| fallback 조건 | cache miss, cache insufficient, cache mismatch |
| fallback 절차 | 카테고리 식별 → SELECT 조회 → cache 보강 반영 → reason 기록 |
| fallback 실패 | 부분 cache 있으면 warning+진행, 전무+DB불가면 실패 |
| 6단계 보강 | machine_spec 생성 시 cache+fallback 결과 활용하여 unresolved 감소 |
| manifest 기록 | hit/miss_categories, db_fallback_used/reason/queries/status |

## 5. Build 체인 보강

**수정 파일**: `.claude/commands/build.md`

| 항목 | 내용 |
|------|------|
| 4단계 | "Convention Cache 참조 검증 **및 DB Fallback**"으로 확장 |
| cache hit | 검증 통과 |
| cache miss/insufficient | DB fallback 판정 → 필요한 항목만 SELECT 조회 |
| fallback 절차 | 카테고리 식별 → SELECT → 검증 수행 → cache 보강(warning) → reason 기록 |
| fallback 실패 | warning 후 해당 검증 skip (cache_insufficient: true) |
| manifest 기록 | hit/miss_categories, mismatch_detected, db_fallback_used/reason/status |

## 6. Fix 체인 보강

**수정 파일**: `.claude/commands/fix.md`, `.claude/skills/system-fix.md`

| 항목 | 내용 |
|------|------|
| 4-1단계 | "Convention Cache 기반 원인 조회 **및 DB Fallback**"으로 확장 |
| cache hit | 원인 확정, DB 불필요 |
| cache miss/insufficient/mismatch | DB fallback → 원인 분석 보강 |
| fallback 절차 | 카테고리 식별 → SELECT → 원인 분석 반영 → cache-DB 차이 기록 → reason 기록 |
| fallback 실패 | "근거 부족"으로 기록, system-fix 위임 검토 |
| system-fix 보강 | DB fallback 결과와 cache 차이를 구조적 오류 근거로 활용 |

## 7. Runtime Policy 보정

| 파일 | 변경 |
|------|------|
| `runtime.yml` | cache.terminology 블록 추가 (5개 용어 정의), db_fallback 설명 확장 |
| `db_access.yml` | db_fallback 섹션 확장: record_fields, cache_update_after_fallback, failure_handling |

## 8. Schema/Manifest 확장

### Schema 추가 필드

| Schema | 추가 필드 |
|--------|-----------|
| `analyze_manifest` | hit_categories, miss_categories, db_fallback_reason[], db_fallback_queries[], db_fallback_status |
| `build_manifest` | hit_categories, miss_categories, mismatch_detected, db_fallback_reason[], db_fallback_status |
| `fix_manifest` | hit_categories, miss_categories, insufficient, db_fallback_reason[], db_fallback_status |
| `verify_result` | cache_errors[] 추가 |

### Manifest 샘플 갱신

| 샘플 | DB fallback 사례 |
|------|-------------------|
| `analyze.manifest.yml` | trigger miss → DB fallback success |
| `build.manifest.yml` | 전체 hit, fallback 미사용 |
| `build verify_result.yml` | table/code/function cache check: found |
| `fix.manifest.yml` | code miss + mismatch → DB fallback(PS.090) success |
| `fix verify_result.yml` | PS.090 not_found → DB fallback 보완 warning |

## 9. 남은 한계 및 미해결 항목

| 항목 | 상태 |
|------|------|
| DB 접속 실제 구현 | psql 명령은 명시됨, 실행 메커니즘은 테스트 필요 |
| cache *.txt 실제 데이터 | 여전히 비어 있음 — /cache-refresh 또는 /analyze로 최초 갱신 필요 |
| fallback 쿼리 자동 생성 | 현재는 수동으로 필요 항목을 SELECT. 자동화는 후속 |
| cache 유효기간 정책 | 미정의 — 현재는 analyze마다 무조건 refresh |

## 10. 다음 단계 권장 작업

1. **최초 cache refresh 실행**: `/cache-refresh` 실행하여 *.txt 채우기
2. **샘플 PPT로 /analyze 테스트**: cache refresh → PPT 분석 → spec 생성 전체 흐름
3. **fallback 쿼리 템플릿화**: 자주 쓰이는 DB fallback SELECT를 sql/ 하위에 준비
4. **cache 유효기간 정책**: refresh 시각 기록 + 일정 시간 경과 시 자동 재판정

## 11. 수정 파일 목록 (20건)

| 분류 | 파일 | 변경 |
|------|------|------|
| Command | `analyze.md` | cache refresh psql 명시, 2-1 DB fallback 단계 추가, machine_spec 보강 |
| Command | `build.md` | cache 검증+DB fallback 절차 확장 |
| Command | `fix.md` | cache 조회+DB fallback 절차 확장 |
| Skill | `system-fix.md` | DB fallback 결과 활용 명시 |
| Config | `runtime.yml` | cache.terminology 블록 추가 |
| Policy | `db_access.yml` | db_fallback 확장 (record_fields, cache_update, failure_handling) |
| Schema | `analyze_manifest.schema.json` | hit/miss_categories, fallback reason/queries/status |
| Schema | `build_manifest.schema.json` | hit/miss_categories, mismatch, fallback reason/status |
| Schema | `fix_manifest.schema.json` | hit/miss_categories, insufficient, fallback reason/status |
| Schema | `verify_result.schema.json` | cache_errors[] 추가 |
| Sample | `analyze.manifest.yml` | trigger fallback 사례 |
| Sample | `build.manifest.yml` | 전체 hit, no fallback 사례 |
| Sample | `build verify_result.yml` | function cache check 추가 |
| Sample | `fix.manifest.yml` | code miss+mismatch, fallback 사례 |
| Sample | `fix verify_result.yml` | PS.090 fallback warning 사례 |
| Report | `cache_db_fallback_integration_report.md` | 본 보고서 |
