# /cache-refresh command

## 목적
`system/cache/convention/` 의 convention cache를 DB에서 최신 데이터로 갱신한다. analyze/build/fix와 독립적으로 언제든 실행 가능하다.

## 실행 전 확인

1. **Lock 확인**: `work/.lock` == `UNLOCKED`. 아니면 즉시 중단.
2. **SQL 파일 확인**: `system/cache/convention/sql/` 에 아래 5개 SQL 파일이 존재하는지 확인.
   - `code_select.sql`
   - `table_select.sql`
   - `view_select.sql`
   - `function_select.sql`
   - `trigger_select.sql`

## 수행 절차

### 1. Lock 획득
```
work/.lock ← "LOCKED:system:cache-refresh"
```

### 2. Cache Refresh 실행
아래 5개를 순서대로 실행한다:

| SQL | 결과 저장 |
|-----|-----------|
| `system/cache/convention/sql/code_select.sql` | `system/cache/convention/code.txt` |
| `system/cache/convention/sql/table_select.sql` | `system/cache/convention/table.txt` |
| `system/cache/convention/sql/view_select.sql` | `system/cache/convention/view.txt` |
| `system/cache/convention/sql/function_select.sql` | `system/cache/convention/function.txt` |
| `system/cache/convention/sql/trigger_select.sql` | `system/cache/convention/trigger.txt` |

각 SQL은 `system/policies/runtime/db_access.yml`의 connection 정보와 규칙을 따른다:
```bash
PGPASSWORD='sjinc12#$' psql -h 10.10.1.100 -p 5466 -U sjinc -d GDI_SERVICE -f {sql_file} -o {output_file} -t -A
```
- SELECT만 허용 (INSERT/UPDATE/DELETE/DDL 금지)
- 조회 전용

### 3. 결과 검증
- 각 *.txt 파일이 0 bytes가 아닌지 확인
- 0 bytes이면 해당 카테고리에 대해 warning 기록

### 4. Lock 해제
```
work/.lock ← "UNLOCKED"
```

### 5. 결과 보고
- 갱신된 파일 목록과 각 파일 크기
- 실패한 카테고리 (있으면)
- "cache refresh 완료" 안내

## 실패 조건
- Lock 충돌 → 즉시 중단
- SQL 파일 누락 → 즉시 중단
- DB 접속 불가 → 실패 보고 (기존 cache 유지)

## 성공 조건
- 5개 *.txt 파일 모두 갱신됨 (또는 일부 warning과 함께 완료)

## 주의
- 이 command는 active_context의 phase를 변경하지 않는다.
- 어떤 상태에서든 실행 가능하다 (idle, task_analyzed, task_built 등).
- analyze는 시작 시 자동으로 이 작업을 수행하므로, 별도 실행은 cache만 미리 갱신하고 싶을 때 사용한다.
