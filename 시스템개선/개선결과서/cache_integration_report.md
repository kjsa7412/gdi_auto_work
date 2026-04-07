# Convention Cache 실행 체인 통합 보고서

## 1. 작업 개요
convention cache(`system/cache/convention/`)를 단순 보관 디렉터리가 아니라, analyze/build/fix 실행 체인에서 실제로 갱신되고 참조되는 운영 자산으로 연결하는 수정 작업을 수행하였다.

## 2. 현재 문제 인식 (수정 전)
- /analyze에 cache refresh 절차 없음 (cache 언급 1회뿐)
- /build, /fix에 cache 참조 조건 미명시
- manifest에 cache 관련 필드 없음
- db_access.yml이 선언적이나 실행 절차에 미연결
- cache *.txt 파일 모두 0 bytes (비어 있음)

## 3. Convention Cache 역할 재정의

| 파일 | 역할 |
|------|------|
| `code.txt` | 공통코드 정의 (sy_code_mst/sy_code_dtl) |
| `table.txt` | 테이블 정의 (컬럼, PK, 인덱스) |
| `view.txt` | 뷰 정의 |
| `function.txt` | 함수/프로시저 정의 (fn_*, sp_*) |
| `trigger.txt` | 트리거 정의 |
| `sql/*.sql` | 위 cache를 갱신하기 위한 조회 전용 SQL |

**cache-first 원칙**: 모든 데이터 조회는 cache를 먼저 확인. DB 직접 조회는 cache 부족 시에만.

## 4. Analyze 체인 연결

**수정 파일**: `.claude/commands/analyze.md`

추가된 절차 — **"### 2. Convention Cache Refresh"**:
- 판정: cache *.txt 중 비어 있거나 누락되면 refresh
- 수행: sql/*.sql → DB 실행 → *.txt 저장
- 실패: 기존 cache로 계속 또는 cache 없이 진행 (warning)
- 기록: analyze.manifest에 cache.refresh_attempted/status/refreshed_files

## 5. Build 체인 연결

**수정 파일**: `.claude/commands/build.md`

추가된 절차 — **"### 4. Convention Cache 참조 검증"**:
- main_table이 table.txt에 존재하는지
- 코드 타입이 code.txt에 정의되어 있는지
- 함수 참조가 function.txt에 존재하는지
- cache 부재 시: warning + skip (build는 refresh 안 함)
- 기록: build.manifest에 cache.used/categories_used/insufficient/db_fallback_used

## 6. Fix 체인 연결

**수정 파일**: `.claude/commands/fix.md`, `.claude/skills/system-fix.md`

추가된 절차 — **"### 4-1. Convention Cache 기반 원인 조회"**:
- naming/DDL/SQL ID 오류 시 cache 먼저 조회
- cache로 해결 가능하면 DB fallback 없이 진행
- 기록: fix.manifest에 cache.used/categories_used/mismatch_detected/db_fallback_used

system-fix에 추가:
- 오류 분류에 "convention cache 불일치" 유형 추가
- 보완 대상에 `system/cache/convention/*.txt` 및 `sql/*.sql` 추가

## 7. Runtime Policy 보정

| 파일 | 변경 |
|------|------|
| `system/config/runtime.yml` | `cache:` 블록 추가 (path, principle, refresh_command, read_only_commands) |
| `system/policies/runtime/db_access.yml` | 전면 재작성: cache_first_principle, cache_refresh 규칙, db_fallback 조건, DB-001~006 |
| `system/policies/runtime/allowed_paths.yml` | analyze에 cache 쓰기 허용, build/fix에 cache 읽기 허용 |
| `system/policies/runtime/context_isolation.yml` | task/fix readable에 `system/cache/convention/**` 추가 |

## 8. Manifest/Schema 확장

| Schema | 추가 필드 |
|--------|-----------|
| `analyze_manifest.schema.json` | `cache.refresh_attempted/required/status/refreshed_files/db_fallback_used` |
| `build_manifest.schema.json` | `cache.used/categories_used/insufficient/db_fallback_used` |
| `fix_manifest.schema.json` | `cache.used/categories_used/mismatch_detected/db_fallback_used` |
| `verify_result.schema.json` | `cache_checks[]`, `cache_warnings[]` |

## 9. 샘플 Manifest 갱신 결과

| 샘플 | cache 사례 |
|------|-----------|
| `analyze.manifest.yml` | refresh 성공, 5개 파일 갱신 |
| `build.manifest.yml` | cache 참조(code/table/function), insufficient: false |
| `build verify_result.yml` | table/code cache_checks: found |
| `fix.manifest.yml` | cache 참조(table/code), mismatch: false |
| `fix verify_result.yml` | table cache_check: found |

## 10. 남은 한계 및 미해결 항목

| 항목 | 상태 |
|------|------|
| DB 접속 실제 구현 | 미구현 — SQL 파일은 준비됨, DB 연결 로직 필요 |
| cache *.txt 실제 데이터 | 비어 있음 — DB refresh 실행 필요 |
| cache refresh 자동화 | analyze에서 판정만 정의, 실제 실행 메커니즘 미구현 |
| cache 유효기간/만료 정책 | 미정의 — 현재는 비어 있으면 refresh |

## 11. 다음 단계 권장 작업

1. **DB 접속 구현**: PostgreSQL 연결 → sql/*.sql 실행 → *.txt 적재
2. **실제 cache 갱신**: 최초 1회 수동 refresh로 *.txt 채우기
3. **cache 유효기간 정책**: 마지막 refresh 시각 기록, 일정 시간 경과 시 자동 refresh 판정
4. **verify에 cache 검증 항목 구체화**: 컬럼 존재, 코드 유효성 등 세부 check
