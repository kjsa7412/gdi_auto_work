# /fix command

## 목적
fix 요청을 분석하여 영향 범위 판정 후 패치/재생성/정책 보완을 수행한다. 재발 방지를 위해 system-fix skill 연계를 항상 검토한다.

## 실행 전 확인

1. **Lock**: `work/.lock` == `UNLOCKED`.
2. **상태**: `current.phase` == `task_built`. 아니면 "build 미완료" 안내 후 중단.
3. **입력**: `work/fix/1.Prep/`에 fix 요청 파일 존재 (.md, .txt, .png 등).
4. **Task 산출물**: `work/task/2.Working/**`, `work/task/3.Result/**` 존재 확인.
5. **금지**: `archive/**` 입력 금지. `work/task/**` 직접 수정 금지 (읽기 전용 참조만).

## 수행 절차

### 1. Workspace 잠금
`work/.lock` ← `LOCKED:fix:fix`, active_context 갱신

### 2. fix.manifest 초기화
`work/fix/2.Working/manifests/fix.manifest.yml` (status: in_progress)

### 3. Fix 요청 분석
`work/fix/1.Prep/` 파일 읽기 및 내용 분석

### 4. 오류 유형 분류 (issue_type)
- **output_patch**: 산출물 단순 수정 (컬럼 순서, 라벨 등)
- **skeleton_patch**: skeleton 템플릿 구조 문제
- **reanalyze**: 화면유형 오분류 등 분석부터 재실행 필요
- **rebuild**: machine_spec 수정 후 build 재실행 필요

### 5. 영향 범위 판정
reanalyze_required, rebuild_required, policy_patch_required, affected_paths 결정

### 6. System-fix Skill 연계
policy_patch_required==true이거나 반복 오류 가능성이 있으면 `system-fix` skill 호출.
policy/template/skeleton 보완 제안 수신.

### 7. 수정 수행
- output_patch: `work/fix/2.Working/patched/`에 수정 파일 → `work/fix/3.Result/deliverables/`
- rebuild: "machine_spec 수정 후 /build 재실행 필요" 안내
- reanalyze: "people_spec 수정 후 /analyze + /build 재실행 필요" 안내
- skeleton_patch: system-fix 결과에 따라 보정안 제시

### 8. Verify Result 생성
`work/fix/2.Working/manifests/verify_result.yml` — patch_applied, no_regression

### 9. fix.manifest 완료
status: completed, issue_type, impact_scope, actions_taken

### 10. Active Context 갱신
current.phase: fix_applied, fix.status: applied, lock ← UNLOCKED

### 11. 결과 보고
fix 유형, 영향 범위, 수행 조치, 다음 단계 안내

## 실패/성공 조건
실패: 입력 없음, task 산출물 없음, 판정 불가 → failed
성공: fix.manifest + verify_result 존재, 영향 범위 명확화
