# /clean command

## 역할
work 전체를 archive로 이동하고 workspace를 초기화하는 **오케스트레이터**.

## 진입 조건
`system/config/runtime.yml` → `commands.clean.start_condition` 참조.
- phase: 어떤 상태에서든 실행 가능
- lock: UNLOCKED
- work 하위에 내용이 없으면 "정리할 작업 없음" 안내

## 필수 정책 로드

| 정책 | 역할 |
|------|------|
| `system/config/runtime.yml` | 진입 조건, 상태 전이, clean 절차 |
| `system/policies/runtime/context_isolation.yml` | lock 형식 |
| `system/policies/runtime/lifecycle.yml` | archive 정책, zone 규칙 |
| `system/config/paths.yml` | 경로 체계 |

## 수행 절차

### 1. Lock 획득
`context_isolation.yml` lock 형식: `LOCKED:system:clean`

### 2. Archive 이동
`lifecycle.yml` → `clean.strategy`: move-all-then-reset
`runtime.yml` → `commands.clean.steps` 순서대로:
- work 하위 전체 → `archive/work_{YYYYMMDD_HHMMSS}/`
- 디렉토리 구조 보존

### 3. Work 디렉토리 재생성
`paths.yml` 경로 체계에 따라 빈 디렉토리 구조 재생성.

### 4. Active Context 초기화 + Lock 해제
`runtime.yml` → `commands.clean.success_state`: idle

### 5. 결과 보고
archive 경로, 보관 파일 수, "새 작업: /analyze" 안내.

## 금지 사항
`lifecycle.yml` 참조:
- archive 내 기존 파일 삭제 금지
- archive를 실행 입력으로 읽기 금지
- 일부만 정리 금지 (전체 이동)
