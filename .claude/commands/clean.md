# /clean command

## 목적
work 전체를 archive로 이동하고 workspace를 초기화한다. **archive 보관을 먼저 수행한 후 reset** (archive-first 원칙).

**archive는 history only. 어떤 command도 archive를 실행 입력으로 사용 불가.**

## 실행 전 확인

1. **Lock**: `work/.lock` == `UNLOCKED`.
2. **상태**: `current.phase` != `idle`. idle이면 "정리할 작업 없음" 안내.
3. **work 내용**: `work/task/` 또는 `work/fix/` 하위에 파일 존재 확인.

## 금지 사항
- archive 내 기존 파일 삭제 금지
- archive를 실행 입력으로 읽기 금지
- 일부만 정리하고 나머지 active 잔존 금지 (전체 이동)

## 수행 절차

### 1. Workspace 잠금
`work/.lock` ← `LOCKED:system:clean`

### 2. Archive 타임스탬프
`YYYYMMDD_HHMMSS` 형식

### 3. Archive 이동
```
archive/work_{timestamp}/task/ ← work/task/ (전체)
archive/work_{timestamp}/fix/ ← work/fix/ (전체)
```
디렉토리 구조 보존 (1.Prep, 2.Working, 3.Result)

### 4. Work 디렉토리 재생성
빈 디렉토리 구조 재생성:
- work/task/1.Prep/, 2.Working/(manifests, original, final, extracted, classified, mapped, generated), 3.Result/(review, deliverables, report)
- work/fix/1.Prep/, 2.Working/(manifests, analyzed, patched, regenerated), 3.Result/(deliverables, report)

### 5. Active Context 초기화
`work/.active_context.yml` → idle 초기 상태로 리셋 (모든 필드 null/empty)

### 6. Lock 해제
`work/.lock` ← `UNLOCKED`

### 7. 결과 보고
- archive 경로, 보관 파일 수, workspace 초기화 완료
- "새 작업: work/task/1.Prep에 PPT 배치 후 /analyze 실행" 안내

## 성공 조건
- archive에 work 보존, work 빈 구조 초기화, phase==idle, lock==UNLOCKED
