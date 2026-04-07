# Manifest 계약화 보고서

## 1. 작업 개요

본 작업은 "manifest 계약화 및 schema 정의 단계"로, analyze/build/fix/clean 흐름의 입출력, 상태전이, 검증 규칙을 기계적으로 검증 가능한 계약층으로 정의한 것이다.

- **작업 일자**: 2026-04-05
- **작업 단계**: Manifest 계약화 및 Schema 정의 (명령어 구현 전 단계)
- **원칙**: single-active workspace, archive 입력 금지

---

## 2. 입력으로 사용한 이전 단계 결과물

| 파일 | 용도 |
|------|------|
| `system/config/paths.yml` | 경로 구조 확인 |
| `system/config/runtime.yml` | 기존 상태 전이 모델 확인 |
| `system/config/framework_manifest.yml` | schema 설계 근거 |
| `system/policies/framework/template_selection.yml` | build 흐름 근거 |
| `system/policies/framework/skeleton_contract.yml` | placeholder 계약 근거 |
| `system/templates/screen-types/*.yml` | 화면 유형 정의 근거 |
| `framework_analysis_report.md` | 프레임워크 분석 근거 |
| `skeleton_extraction_report.md` | skeleton 설계 근거 |

---

## 3. 실행 단계별 계약 정의 요약

### analyze
| 항목 | 값 |
|------|-----|
| 목적 | PPT에서 화면 정보 추출 → people_spec/machine_spec 생성 |
| 시작 조건 | idle/task_prepared + 1.Prep에 PPT 존재 |
| 필수 입력 | `work/task/1.Prep/*.pptx` |
| 필수 출력 | `original/people_spec.md`, `original/machine_spec.yml`, `final/people_spec.md`, `analyze.manifest.yml` |
| 성공 상태 | task_analyzed |
| 실패 상태 | failed |

### build
| 항목 | 값 |
|------|-----|
| 목적 | people_spec + machine_spec → machine_spec(final) + 코드 산출물 + 선택적 deploy 준비 |
| 시작 조건 | task_analyzed + analyze.manifest 존재 |
| 필수 입력 | `original/people_spec.md`, `original/machine_spec.yml`, `final/people_spec.md` |
| 필수 출력 | `final/machine_spec.yml`, `build.manifest.yml`, `verify_result.yml`, `3.Result/deliverables/**` |
| 선택 출력 | `3.Result/report/deploy_summary.yml` |
| 성공 상태 | task_built |
| unresolved 시 | build 차단 |
| deploy 정책 | verify_result pass/warn 시 prepare-only 모드로 deploy skill 호출. deploy 실패 ≠ build 실패 |

### fix
| 항목 | 값 |
|------|-----|
| 목적 | fix 요청 분석 → 영향 범위 판정 → 패치/재생성 |
| 시작 조건 | task_built + fix/1.Prep에 요청 파일 존재 |
| task 참조 | 읽기 전용 (task 산출물 수정 금지) |
| 필수 출력 | `fix.manifest.yml`, `verify_result.yml` |
| 성공 상태 | fix_applied |
| issue_type | output_patch / skeleton_patch / reanalyze / rebuild |

### clean
| 항목 | 값 |
|------|-----|
| 목적 | work → archive 이동 + 초기화 |
| 시작 조건 | 모든 상태 (idle 제외) |
| 동작 | move-all-then-reset |
| 성공 상태 | idle |

---

## 4. Active Context 및 Lock 규칙

**active_context.yml 구조**:
- `workspace_mode`: single-active
- `current.*`: 현재 실행 중인 command 상태
- `task.*`: task 작업 상태 (status, spec 경로, 타임스탬프)
- `fix.*`: fix 작업 상태
- `history.*`: 마지막 성공/실패 이력

**lock 형식**: `UNLOCKED | LOCKED:{type}:{command}`
- command 시작 시 lock 획득
- command 완료(성공/실패) 시 lock 해제
- lock 상태에서 다른 command 실행 금지

상세: `system/schemas/active_context.schema.json`, `system/policies/runtime/context_isolation.yml`

---

## 5. people_spec / machine_spec 계약 요약

| 항목 | people_spec | machine_spec |
|------|-------------|-------------|
| 목적 | 사람 검토용 문서 | build 직접 입력 |
| 형식 | Markdown (YAML frontmatter) | YAML |
| 생성 시점 | analyze | analyze (original), build (final) |
| 수정 주체 | 사용자 (final) | 시스템 |
| 검증 | 최소 섹션 존재 + meta 블록 | 전체 필드 구조 검증 |
| 주요 내용 | 화면 개요, 필드 설명, 비즈니스 규칙 | screen_type, skeleton_choice, placeholders, SQL, artifacts |

템플릿: `system/templates/spec/people_spec.md`, `system/templates/spec/machine_spec.yml`
Schema: `system/schemas/people_spec.schema.json`, `system/schemas/machine_spec.schema.json`

---

## 6. Schema 작성 결과 요약

| Schema | 파일 | 검증 대상 |
|--------|------|-----------|
| active_context | `system/schemas/active_context.schema.json` | `work/.active_context.yml` |
| analyze_manifest | `system/schemas/analyze_manifest.schema.json` | analyze 단계 manifest |
| build_manifest | `system/schemas/build_manifest.schema.json` | build 단계 manifest |
| fix_manifest | `system/schemas/fix_manifest.schema.json` | fix 단계 manifest |
| verify_result | `system/schemas/verify_result.schema.json` | self-check 결과 (build/fix) |
| people_spec | `system/schemas/people_spec.schema.json` | people_spec.md frontmatter |
| machine_spec | `system/schemas/machine_spec.schema.json` | machine_spec.yml 전체 |
| framework_manifest | `system/schemas/framework_manifest.schema.json` | framework_manifest.yml |

---

## 7. Runtime Policy 보정 결과

| 파일 | 보정 내용 |
|------|-----------|
| `system/config/runtime.yml` | 전면 재작성: 상태 전이 모델 10개 상태, commands 4개 계약, 입출력 경로 |
| `system/policies/runtime/allowed_paths.yml` | mandatory_outputs 추가, task_reference_mode 추가 |
| `system/policies/runtime/context_isolation.yml` | lock 형식 명세, active_context 갱신 시점 명세 |
| `system/policies/runtime/lifecycle.yml` | mandatory_output_check 추가, unresolved_policy 추가, failed_recovery 추가 |

---

## 8. 샘플 Manifest 정합성 검증 결과

| 샘플 | Schema | 결과 | 비고 |
|------|--------|------|------|
| `work/task/.../analyze.manifest.yml` | analyze_manifest.schema.json | OK | 필수 필드 모두 존재 |
| `work/task/.../build.manifest.yml` | build_manifest.schema.json | OK | 필수 필드 모두 존재 |
| `work/task/.../verify_result.yml` | verify_result.schema.json | OK | stage: build, status: pass |
| `work/fix/.../fix.manifest.yml` | fix_manifest.schema.json | OK | issue_type: output_patch |
| `work/fix/.../verify_result.yml` | verify_result.schema.json | OK | stage: fix, status: pass |
| `work/.active_context.yml` | active_context.schema.json | OK | idle 상태 |
| `system/templates/spec/people_spec.md` | people_spec.schema.json | OK | meta 블록 + 섹션 |
| `system/templates/spec/machine_spec.yml` | machine_spec.schema.json | OK | 필수 필드 모두 존재 |

**runtime.yml ↔ policies 정합성**: OK
- lifecycle.yml의 mandatory_output_check가 allowed_paths.yml의 mandatory_outputs와 일치
- context_isolation.yml의 lock 규칙이 runtime.yml의 state_machine과 일치
- failed 복구 경로가 lifecycle.yml과 runtime.yml에서 일관 (clean 필수)

---

## 9. 아직 미해결인 항목

| 항목 | 상태 | 비고 |
|------|------|------|
| PPT 파싱 로직 | 미구현 | analyze command에서 실제 PPT 읽기 방식 결정 필요 |
| placeholder 치환 엔진 | 미구현 | build command에서 배열형 placeholder → 코드 블록 변환 |
| DB convention cache 적재 | 미구현 | SQL은 준비됨, 실제 DB 접속 및 적재 로직 필요 |
| user_review 자동 감지 | 미확정 | 사용자가 review 완료를 어떻게 시스템에 알리는지 |
| verify_result 검증 항목 상세 | 미확정 | self-check 항목의 상세 검증 로직 |
| deploy 로직 | 구현 완료 | build 후속 prepare-only, deploy_policy.yml, deploy_summary.schema.json 추가 |
| test 실행 로직 | 이번 단계 범위 외 | |
| system/policies/runtime/db_access.yml | 미작성 | DB 접근 정책 (convention cache 관련) |

---

## 10. 다음 단계에서 해야 할 작업

1. **`.claude/commands/analyze.md` 작성**: analyze 실행 프롬프트 (PPT 파싱 → spec 생성)
2. **`.claude/commands/build.md` 작성**: build 실행 프롬프트 (spec → 코드 생성)
3. **`.claude/commands/fix.md` 작성**: fix 실행 프롬프트 (영향 분석 → 패치)
4. **`.claude/commands/clean.md` 작성**: clean 실행 프롬프트 (archive → reset)
5. **placeholder 치환 엔진 구현**: machine_spec → skeleton 치환 로직
6. **verify 검증 로직 구현**: self-check 항목별 검증 로직
7. **user_review 흐름 확정**: 사용자 검토 완료 시그널 방식

---

## 11. 작성/수정 파일 목록

### Config 보정 (1건)
| 파일 | 변경 |
|------|------|
| `system/config/runtime.yml` | 전면 재작성 (상태 전이, 계약 정의) |

### Runtime Policy 보정 (3건)
| 파일 | 변경 |
|------|------|
| `system/policies/runtime/allowed_paths.yml` | mandatory_outputs, task_reference_mode 추가 |
| `system/policies/runtime/context_isolation.yml` | lock 형식, 갱신 시점 명세 |
| `system/policies/runtime/lifecycle.yml` | mandatory_output_check, unresolved_policy 추가 |

### Schema 신규 생성 (8건)
| 파일 |
|------|
| `system/schemas/active_context.schema.json` |
| `system/schemas/analyze_manifest.schema.json` |
| `system/schemas/build_manifest.schema.json` |
| `system/schemas/fix_manifest.schema.json` |
| `system/schemas/verify_result.schema.json` |
| `system/schemas/people_spec.schema.json` |
| `system/schemas/machine_spec.schema.json` |
| `system/schemas/framework_manifest.schema.json` |

### Manifest 샘플 (5건)
| 파일 |
|------|
| `work/task/2.Working/manifests/analyze.manifest.yml` |
| `work/task/2.Working/manifests/build.manifest.yml` |
| `work/task/2.Working/manifests/verify_result.yml` |
| `work/fix/2.Working/manifests/fix.manifest.yml` |
| `work/fix/2.Working/manifests/verify_result.yml` |

### Context/Lock (2건)
| 파일 |
|------|
| `work/.active_context.yml` |
| `work/.lock` |

### Spec 템플릿 (2건)
| 파일 |
|------|
| `system/templates/spec/people_spec.md` |
| `system/templates/spec/machine_spec.yml` |

### 보고서 (1건)
| 파일 |
|------|
| `manifest_contract_report.md` |

**총 22건** (보정 4 + 신규 18)
