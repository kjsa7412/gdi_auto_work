# Command 구현 보고서

## 1. 작업 개요

본 작업은 ".claude command 및 skill 구현 단계"로, 이전 단계에서 정의한 manifest/schema/runtime 계약을 바탕으로 Claude Code가 실행할 수 있는 /analyze, /build, /fix, /clean command와 generate, deploy, system-fix skill 프롬프트를 작성한 것이다.

- **작업 일자**: 2026-04-05
- **작업 단계**: Command/Skill 구현 (실제 실행 전 단계)

---

## 2. 입력으로 사용한 이전 단계 결과물

| 파일 | 용도 |
|------|------|
| `system/config/paths.yml` | 경로 구조 → command 내 경로 참조 |
| `system/config/runtime.yml` | 상태 전이 → command 진입/종료 조건 |
| `system/policies/runtime/*.yml` | 접근 통제 → command 금지/허용 경로 |
| `system/policies/framework/*.yml` | 생성 규칙 → generate skill 참조 |
| `system/schemas/*.json` | 검증 규칙 → manifest/spec 검증 |
| `system/templates/spec/*` | 템플릿 → analyze에서 spec 생성 |
| `system/templates/skeletons/**` | skeleton → generate에서 코드 생성 |
| `manifest_contract_report.md` | 계약 정의 → command 설계 근거 |

---

## 3. Command 구현 방침

- **command = orchestration**: 진입 조건 확인 → 실행 → manifest 갱신 → 결과 보고
- **skill = 세부 실행**: 특정 하위 작업 수행 (코드 생성, 배포 준비, 시스템 보정)
- **모든 command**: lock 확인 → active_context 갱신 → 작업 → manifest 갱신 → lock 해제
- **실패 시**: 즉시 중단, phase → failed, lock 해제, 에러 기록

---

## 4. /analyze 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/commands/analyze.md` |
| 진입 | idle/task_prepared + PPT 존재 |
| 핵심 | PPT → people_spec + machine_spec 생성 |
| 출력 | original/people_spec.md, original/machine_spec.yml, final/people_spec.md, analyze.manifest.yml |
| 종료 | task_analyzed |
| 참조 | paths.yml, naming.yml, framework_manifest.yml, screen-types/*.yml, spec templates |

---

## 5. /build 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/commands/build.md` |
| 진입 | task_reviewed |
| 핵심 | people_spec diff → machine_spec(final) → **generate skill** → 코드 생성 |
| 출력 | final/machine_spec.yml, generated/*, verify_result.yml, build.manifest.yml, deliverables/* |
| 종료 | task_built |
| 참조 | template_selection, skeleton_contract, skeletons/**, framework policies |

**build → generate skill 연결**: build가 final/machine_spec를 확정한 후 generate skill 호출. generate가 skeleton 치환 + self-check 수행 후 결과 반환.

---

## 6. /fix 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/commands/fix.md` |
| 진입 | task_built + fix/1.Prep에 요청 존재 |
| 핵심 | 영향 분석 → 유형 분류 → 패치/안내 → **system-fix skill** 연계 |
| 출력 | fix.manifest.yml, verify_result.yml, patched/*, deliverables/* |
| 종료 | fix_applied |
| 참조 | task 산출물 (읽기 전용), framework policies |

**fix → system-fix skill 연결**: policy_patch_required이거나 반복 오류 가능성이 있을 때 system-fix 호출. 시스템 파일 보완 제안 수신.

---

## 7. /clean 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/commands/clean.md` |
| 진입 | idle 외 모든 상태 |
| 핵심 | work → archive 이동 → workspace 초기화 |
| 종료 | idle |
| 원칙 | archive-first (보관 먼저, 삭제는 하지 않음) |

---

## 8. generate skill 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/skills/generate.md` |
| 호출자 | build command |
| 핵심 | machine_spec → skeleton 선택 → placeholder 치환 → artifact 생성 → self-check |
| 절대 규칙 | skeleton 기반만 허용. 자유 생성 금지 |
| 반환 | generated_paths, self_check, violations |

---

## 9. deploy skill 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/skills/deploy.md` |
| 핵심 | deliverables → 반영 대상 정리 → overwrite 경고 → (사용자 확인 후) 복사 |
| 원칙 | 환경 미확정 시 "준비/패키징" 수준. 무조건 배포하지 않음 |

---

## 10. system-fix skill 설계 요약

| 항목 | 값 |
|------|-----|
| 파일 | `.claude/skills/system-fix.md` |
| 호출자 | fix command |
| 핵심 | 반복/구조적 오류 원인 분류 → 보완 대상 식별 → patch proposal |
| 대상 | skeleton, policy, template, naming, runtime rule |
| 원칙 | 근거 기반만. 추정 변경 금지 |

---

## 11. settings.json 반영 내용

`.claude/settings.json`에 아래 반영:
- permissions: Read, Glob, Grep, Edit, Write, Bash(*)
- project.principles: single-active, skeleton 기반, archive history only
- project.workflow.commands: analyze/build/fix/clean 경로
- project.workflow.skills: generate/deploy/system-fix 경로
- project.workflow.flow: analyze → review → build → fix → clean

---

## 12. Command-Skill 정합성 검증 결과

| 검증 항목 | 결과 |
|-----------|------|
| build → generate 입출력 연결 | OK: machine_spec → generate → generated_paths 반환 |
| fix → system-fix 역할 구분 | OK: fix=orchestration, system-fix=시스템 보정 |
| clean → lifecycle 충돌 | OK: archive-first, 모든 상태에서 실행 가능 |
| active_context/lock 규칙 | OK: 모든 command에서 lock 확인→획득→해제 패턴 일치 |
| 경로 참조 일치 (paths.yml) | OK: 모든 경로가 paths.yml과 일치 |
| archive 입력 금지 | OK: 모든 command에서 archive 금지 명시 |
| build → deploy 연결 | OK: build 후 선택적으로 deploy 호출 가능 구조 |
| manifest/schema 참조 | OK: 각 command가 해당 schema 검증 명시 |

---

## 13. 아직 미해결인 항목

| 항목 | 상태 |
|------|------|
| PPT 파싱 상세 로직 | 미구현: analyze에서 PPT를 실제로 어떻게 파싱할지 |
| 배열형 placeholder 치환 엔진 | 미구현: generate에서 search_fields → webix 코드 변환 |
| user_review 시그널 | 미확정: 사용자가 review 완료를 어떻게 시스템에 알릴지 |
| DB convention cache 적재 | 미구현: SQL은 준비됨, 적재 로직 필요 |
| deploy 대상 환경 | 미확정: 실제 프로젝트 경로 확정 필요 |
| system/policies/runtime/db_access.yml | 미작성: DB 접근 정책 |

---

## 14. 다음 단계에서 해야 할 작업

1. **실제 테스트 실행**: 샘플 PPT로 /analyze → review → /build → /clean 전체 흐름 테스트
2. **PPT 파싱 로직 구현**: python-pptx 또는 직접 슬라이드 분석 방식 확정
3. **Placeholder 치환 엔진**: machine_spec 배열 → webix/SQL 코드 블록 변환 로직
4. **user_review 흐름**: review 완료 시그널 방식 (수동 상태 변경 또는 별도 command)
5. **Convention cache 적재**: DB 접속하여 코드/테이블/뷰/함수 캐시 파일 생성
6. **Deploy 환경 확정**: 실제 프로젝트 배포 경로 설정

---

## 15. 작성 파일 목록

| 파일 | 설명 |
|------|------|
| `.claude/commands/analyze.md` | /analyze command 프롬프트 |
| `.claude/commands/build.md` | /build command 프롬프트 |
| `.claude/commands/fix.md` | /fix command 프롬프트 |
| `.claude/commands/clean.md` | /clean command 프롬프트 |
| `.claude/skills/generate.md` | generate skill 프롬프트 |
| `.claude/skills/deploy.md` | deploy skill 프롬프트 |
| `.claude/skills/system-fix.md` | system-fix skill 프롬프트 |
| `.claude/settings.json` | Claude Code 설정 (보정) |
| `command_implementation_report.md` | 본 보고서 |

**총 9건**
