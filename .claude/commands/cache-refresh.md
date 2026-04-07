# /cache-refresh command

## 역할
convention cache를 DB에서 최신 데이터로 갱신하는 **독립 실행 command**.

## 진입 조건
- lock: UNLOCKED
- SQL 파일: `system/cache/convention/sql/` 에 5개 SQL 존재
- 어떤 phase에서든 실행 가능 (phase 변경 없음)

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/policies/runtime/db_access.yml` | DB 접속 정보, cache-first 원칙, refresh 규칙 |
| `system/policies/runtime/context_isolation.yml` | lock 형식 |

## 수행 절차

### 1. Lock 획득
`context_isolation.yml` lock 형식: `LOCKED:system:cache-refresh`

### 2. Cache Refresh 실행
`db_access.yml` → `connection` 정보와 `cache_first_principle.refresh_sql_path` 기준으로:

| SQL | 결과 |
|-----|------|
| `sql/code_select.sql` | `code.txt` |
| `sql/table_select.sql` | `table.txt` |
| `sql/view_select.sql` | `view.txt` |
| `sql/function_select.sql` | `function.txt` |
| `sql/trigger_select.sql` | `trigger.txt` |

`db_access.yml` → `rules` DB-002: SELECT만 허용.

### 3. 결과 검증
각 *.txt 0 bytes 확인. 0 bytes → warning.

### 4. Lock 해제 + 결과 보고
갱신 파일 목록, 파일 크기, 실패 카테고리.

## 실패/성공 조건
`db_access.yml` → `cache_refresh.failure_handling` 참조.
