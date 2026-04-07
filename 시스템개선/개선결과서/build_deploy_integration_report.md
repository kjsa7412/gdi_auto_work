# Build-Deploy 연결 통합 보고서

## 1. 작업 개요

본 작업은 `/build` command 마지막에 `deploy` skill을 선택적으로 연결하는 수정 작업이다.
기존 아키텍처를 유지한 상태에서, build의 생성/검증/결과물 정리 이후 deploy prepare-only를 자연스럽게 연결하였다.

- **작업 일자**: 2026-04-05
- **작업 단계**: Build-Deploy 연결 통합 수정

---

## 2. 현재 문제 인식

수정 전 상태:

| 항목 | 상태 |
|------|------|
| /build에 deploy 호출 단계 | 없음 |
| /build 결과 보고 | /fix 또는 /clean만 안내 |
| deploy skill | 존재하나 build에 연결되지 않음 |
| deploy 모드 구분 | prepare-only/apply 구분 없음 |
| build.manifest에 deploy 필드 | 없음 |
| verify_result에 deploy 검증 | 없음 |
| runtime.yml build 섹션 deploy | 미등록 |
| allowed_paths deploy 경로 | 미등록 |
| context_isolation deploy 규칙 | 없음 |
| deploy_policy.yml | 미존재 |
| deploy_summary.schema.json | 미존재 |

---

## 3. Build와 Deploy 역할 재정의

### Build 책임
- review skill 호출 → final/machine_spec.yml 생성
- convention cache 검증 및 필요 시 DB fallback
- generate skill 호출 → skeleton 기반 코드 생성
- verify_result 생성
- deliverables/report 정리
- build.manifest 완료
- **deploy 가능 여부 판정**
- **선택적 deploy 준비 호출 (prepare-only)**

### Deploy 책임
- deliverables 수집 및 존재 검증
- framework_manifest 기반 target path 계산
- overwrite/conflict 검토
- prepare-only: summary 생성만 (실제 복사 없음)
- apply: 사용자 확인 후 실제 복사
- deploy_summary 생성

### 핵심 경계
- **build 성공 ≠ deploy 성공**: build는 생성/검증/deliverables 완료로 성공. deploy 준비 실패는 build를 무효화하지 않음.
- **deploy apply는 별도 확인 필요**: prepare-only는 자동 진행, apply는 사용자 명시적 확인 필수.

---

## 4. /build 보강 내용

**파일**: `.claude/commands/build.md`

### 추가된 단계 (섹션 9)
1. **9-1. Deploy 진입 조건 확인**
   - verify_result.status == pass 또는 warn
   - deliverables 경로에 파일 1개 이상 존재
   - deploy 금지 상태가 아님
   - 미충족 시 deploy.attempted = false로 기록 후 진행

2. **9-2. Deploy Skill 호출 (prepare-only)**
   - deploy skill을 prepare-only 모드로 호출
   - deliverables, framework_manifest, mode 전달
   - 반환: deploy_summary (status, target_paths, warnings, conflicts)

3. **9-3. Deploy 결과 기록**
   - build.manifest의 `deploy` 섹션에 기록
   - deploy.attempted, mode, status, summary_path, target_paths, warnings, conflicts

4. **9-4. Deploy 실패 처리**
   - deploy 실패 → build.manifest.deploy.status만 failed/warn
   - build.manifest.status는 여전히 completed

### 수정된 항목
- 결과 보고(섹션 12)에 deploy 준비 결과 안내 추가
- 실패 조건에 "deploy 실패는 build 실패가 아님" 명시
- 성공 조건에 "deploy는 선택적" 명시

---

## 5. Deploy Skill 보정 내용

**파일**: `.claude/skills/deploy.md`

### 추가된 내용
| 항목 | 변경 |
|------|------|
| 동작 모드 | prepare-only (기본) / apply 이원화 |
| build 호출 시 기본 모드 | prepare-only 명시 |
| 입력에 mode 파라미터 | "prepare-only" 또는 "apply" |
| target path 계산 규칙 | framework_manifest source_roots + supported_artifacts 상세 |
| path_unknown 처리 | 계산 불가 시 status: "path_unknown" 기록 |
| apply 절차 | 사용자 확인 필수, conflict 파일 건너뛰기 |
| 출력 구조 | deploy_result 통일 (mode, status, target_paths, copied_paths, warnings, conflicts) |
| build 연계 | prepare-only 결과를 build.manifest/report에 바로 붙일 수 있는 구조 |

---

## 6. Runtime/Policy 보정 내용

### system/config/runtime.yml
| 변경 | 상세 |
|------|------|
| build.skills_used | deploy skill 추가 (order: 3, optional: true) |
| build.skills_used[deploy].condition | "verify_result.status가 pass 또는 warn이고 deliverables 존재 시" |
| build.skills_used[deploy].default_mode | "prepare-only" |
| build.outputs.optional | deploy_summary.yml 추가 |
| build.deploy_policy | 신규 섹션: entry_condition, default_mode, apply_requires |

### system/policies/runtime/allowed_paths.yml
| 변경 | 상세 |
|------|------|
| build.allowed_inputs | `work/task/3.Result/deliverables/**`, `deploy_policy.yml` 추가 |
| build.allowed_outputs | `deploy_summary.yml` 추가 |
| build.optional_outputs | 신규: `deploy_summary.yml` |
| deploy_paths | 신규 섹션: readable, writable, apply_writable, forbidden |

### system/policies/runtime/context_isolation.yml
| 변경 | 상세 |
|------|------|
| task_deploy | 신규 격리 규칙: deploy skill의 읽기/쓰기 범위 |
| fix_deploy | 신규 격리 규칙: fix 결과물 deploy 시 범위 |

### system/policies/runtime/deploy_policy.yml (신규)
| 항목 | 내용 |
|------|------|
| modes | prepare-only (기본, 확인 불필요), apply (확인 필수) |
| entry_conditions | from_build, from_fix, standalone 각각 정의 |
| target_path_resolution | framework_manifest 기반 규칙 |
| conflict_handling | 모드별 처리 방식 |
| failure_impact | deploy 실패 ≠ build/fix 실패 |
| forbidden | archive 입력 금지, 미확인 apply 금지 |

---

## 7. Schema/Manifest 확장 내용

### system/schemas/build_manifest.schema.json
| 추가 필드 | 타입 | 설명 |
|-----------|------|------|
| deploy.attempted | boolean | deploy 시도 여부 |
| deploy.mode | enum [prepare-only, apply] | 동작 모드 |
| deploy.status | enum [prepared, warn, failed, skipped, applied, partial] | 결과 상태 |
| deploy.summary_path | string\|null | deploy_summary.yml 경로 |
| deploy.total_files | integer | 대상 파일 수 |
| deploy.target_paths[] | array of {source, target, status} | 파일별 매핑 |
| deploy.warnings[] | array of string | 경고 목록 |
| deploy.conflicts[] | array of string | 충돌 목록 |

### system/schemas/verify_result.schema.json
| 추가 필드 | 타입 | 설명 |
|-----------|------|------|
| deploy_checks[] | array of {id, description, result, message} | deploy 검증 항목 |
| deploy_warnings[] | array of string | deploy 관련 경고 |

### system/schemas/deploy_summary.schema.json (신규)
- deploy_summary 전체 구조 정의
- mode, status, total_files, target_paths, warnings, conflicts, copied_paths

---

## 8. 샘플 Manifest 갱신 결과

### work/task/2.Working/manifests/build.manifest.yml
- `deploy` 섹션 추가
- deploy.attempted: true, mode: "prepare-only", status: "prepared"
- target_paths 2건 (DTA030.html, dta030.xml) → target path 계산 완료
- warnings/conflicts 없음
- build.status는 "completed" 유지

### work/task/2.Working/manifests/verify_result.yml
- `deploy_checks` 섹션 추가 (3건: deliverables_exist, target_path_resolved, overwrite_conflict)
- `deploy_warnings` 섹션 추가 (빈 배열)

### work/task/3.Result/report/deploy_summary.yml (신규)
- mode: "prepare-only", status: "prepared"
- target_paths 2건, copied_paths 비어있음 (prepare-only이므로)

---

## 9. 남은 한계 및 미해결 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| deploy apply 사용자 확인 방식 | 미확정 | 별도 command? 대화형 확인? |
| 실제 프로젝트 절대 경로 | 미확정 | framework_manifest의 source_roots는 상대 경로 |
| deploy apply 후 상태 전이 | 미확정 | task_built에서 변경 없음 (deploy는 상태 전이에 영향 없음) |
| fix → deploy 연결 | 구조만 정의 | fix command에 deploy 호출 단계는 아직 미추가 |
| deploy 이력 관리 | 미확정 | 동일 deliverables에 대한 반복 deploy 이력 |

---

## 10. 다음 단계 권장 작업

1. **deploy apply 실행 방식 확정**: 별도 `/deploy` command 또는 build 후 대화형 확인
2. **실제 프로젝트 경로 확정**: deploy target의 절대 경로 기준점 설정
3. **fix → deploy 연결**: /fix command에도 동일 패턴으로 deploy prepare-only 추가
4. **deploy apply 테스트**: 실제 파일 복사 + 충돌 처리 + 결과 기록 검증
5. **전체 흐름 테스트**: analyze → build(+deploy prepare) → fix(+deploy prepare) → clean 전체 순환

---

## 수정/생성 파일 목록

### 수정 (8건)
| 파일 | 변경 요약 |
|------|-----------|
| `.claude/commands/build.md` | 섹션 9 deploy 진입 판정/호출 추가, 결과 보고 보정 |
| `.claude/skills/deploy.md` | prepare-only/apply 이원화, build 연계 인터페이스 명시 |
| `.claude/settings.json` | workflow.flow에 deploy-prepare 반영 |
| `system/config/runtime.yml` | build.skills_used에 deploy 추가, deploy_policy 섹션 |
| `system/policies/runtime/allowed_paths.yml` | deploy 경로 추가, optional_outputs, deploy_paths |
| `system/policies/runtime/context_isolation.yml` | task_deploy, fix_deploy 격리 규칙 추가 |
| `system/schemas/build_manifest.schema.json` | deploy 객체 필드 추가 |
| `system/schemas/verify_result.schema.json` | deploy_checks, deploy_warnings 추가 |

### 샘플 갱신 (2건)
| 파일 | 변경 요약 |
|------|-----------|
| `work/task/2.Working/manifests/build.manifest.yml` | deploy 섹션 추가 |
| `work/task/2.Working/manifests/verify_result.yml` | deploy_checks, deploy_warnings 추가 |

### 신규 생성 (4건)
| 파일 | 설명 |
|------|------|
| `system/policies/runtime/deploy_policy.yml` | deploy 실행 정책 |
| `system/schemas/deploy_summary.schema.json` | deploy_summary schema |
| `work/task/3.Result/report/deploy_summary.yml` | deploy_summary 샘플 |
| `build_deploy_integration_report.md` | 본 보고서 |

### 문서 보정 (2건)
| 파일 | 변경 요약 |
|------|-----------|
| `command_implementation_report.md` | build 설계 요약 보정, deploy skill 요약 보정, 정합성 검증 갱신 |
| `manifest_contract_report.md` | build 시작 조건 보정 (task_reviewed → task_analyzed), deploy 정책 추가 |

**총 16건** (수정 8 + 샘플 갱신 2 + 신규 4 + 문서 보정 2)
