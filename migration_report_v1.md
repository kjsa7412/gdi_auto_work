# v5 → gdi_auto_work 이식 보고서 v1

## 작업 개요
- **작업 일자**: 2026-04-05
- **Source**: `C:\auto_gdi_work_v5`
- **Target**: `C:\gdi_auto_work`
- **목적**: v5의 analyze 품질, skeleton 선택 정확도, framework 강제 규칙을 새 시스템으로 이식

---

## 복사 대상 및 결과

| ID | Source | Target | 결과 |
|----|--------|--------|------|
| A-1 | `policies/analyze/ppt_extraction.yml` | `system/policies/analyze/ppt_extraction.yml` | 완료 — PE-001~PE-006 규칙 전체 복사 (GROUP 재귀탐색, 위치기반 영역분류, 라벨/입력 패턴, row/col/gravity, 좌우분할, 완전성검증) |
| A-2 | `policies/analyze/classify_tags.yml` | `system/policies/analyze/classify_tags.yml` | 완료 — 13개 태그(META~SQL) + CT-001 규칙 |
| A-3 | `docs/templates/screen_spec_template.md` | `system/templates/spec/people_spec.md` | 완료 — v5의 11섹션 템플릿(META, SEARCH, GRID, FORM, BUTTON, VALID, EVENT, NAV, 첨부, 팝업, 특이사항) 기반으로 교체. YAML frontmatter 유지. 화면유형명 TO-BE로 변환 |
| A-4 | `docs/templates/screen_spec_guide.md` | `system/templates/spec/people_spec_guide.md` | 완료 — 경로를 `work/task/1.Prep/`로 변환, 화면유형명 TO-BE로 변환 |

---

## 병합 대상 및 결과

| ID | Source | Target | 결과 |
|----|--------|--------|------|
| B-1 | `policies/framework/layout_rules.yml` | `system/policies/framework/layout_rules.yml` | 완료 — v5_rules 섹션 추가: LR-001(5칸 gravity), LR-002(CSS 클래스), LR-003(data-grid 필수), LR-004(편집 그리드 빈 행), LR-005(wrapper 필수), LR-006(코드헬프 editor) |
| B-2 | `policies/framework/skeleton_contract.yml` + `controller_generation.yml` | `system/policies/framework/skeleton_contract.yml` | 완료 — artifact_structure_contract 섹션 추가: html/xml/controller/service required_structure, SQL audit_columns(정확 접두사 강제), SC-001/SC-002, CG-001~CG-003(CommonController 우선 원칙) |
| B-3 | `policies/framework/template_selection.yml` | `system/policies/framework/template_selection.yml` | 완료 — confidence_rules 섹션 추가: TS-001~TS-003, confidence_threshold 0.8, manual_review_fallback |
| B-4 | `policies/analyze/screen_type_rules.yml` | `system/policies/analyze/screen_type_rules.yml` | 완료 — 신규 생성. 유형명 변환: grid-only→list, search-grid-form→list-detail, form-detail→form, popup-grid→popup. unknown+manual_review 유지 |
| B-5a | `screen-types/grid-only.yml` | `system/templates/screen-types/list.yml` | 완료 — v5_enrichment 섹션 추가: match_conditions, layout, required_functions/listeners, confidence 0.9 |
| B-5b | `screen-types/search-grid-form.yml` | `system/templates/screen-types/list-detail.yml` | 완료 — v5_enrichment 섹션 추가: half-left/half-right layout, createPostForm, gridRow.click, confidence 0.8 |
| B-5c | `screen-types/form-detail.yml` | `system/templates/screen-types/form.yml` | 완료 — v5_enrichment 섹션 추가: sub-screen 조건, content_fill layout, sy_menu_info 제외 주의, confidence 0.8 |
| B-5d | `screen-types/popup-grid.yml` | `system/templates/screen-types/popup.yml` | 완료 — v5_enrichment 섹션 추가: data-modal layout, 확인/취소 footer, topTitleButtonFragment 금지, confidence 0.85 |

---

## 재작성 대상 및 결과

| ID | Source | Target | 결과 |
|----|--------|--------|------|
| C-1 | `policies/runtime/allowed_paths.yml` | `system/policies/runtime/allowed_paths.yml` | 이미 보정 완료 (이전 단계). v5 철학(manifest 기반, archive 금지) 반영 확인 |
| C-2 | `schemas/manifest.schema.json` | `system/schemas/*manifest*.json` | 이미 작성 완료 (이전 단계). analyze/build/fix 별 개별 schema |
| C-3 | `.claude/commands/*` | `.claude/commands/*` | 이미 재작성 완료 (이전 단계). active-context 기반, 무인자 구조 |
| C-4 | `.claude/skills/*` | `.claude/skills/*` | 이미 재작성 완료 (이전 단계). generate/deploy/system-fix/review |
| C-5 | `controller_generation.yml` | `skeleton_contract.yml`에 병합 | 완료 — B-2에서 CG-001~CG-003 규칙을 skeleton_contract에 통합 |

---

## 보류 대상 목록

| ID | Source | 사유 |
|----|--------|------|
| D-1 | `.claude/commands/test.md` | 현재 단계는 analyze/build/fix/clean 안정화 우선. test 체계는 phase 2 |
| D-2 | Playwright 관련 자산 전체 | 실행 검증 시스템은 추후 추가 |
| D-3 | `proposals/policy_changes/*` | system-fix skill 활성화 시 반영 예정 |
| - | `policies/analyze/default_resolution.yml` | 유용하나 현재 analyze command에서 직접 필요하지 않음 |
| - | `policies/analyze/phase_rules.yml` | v5 phase 1~4 구조 전용. 새 시스템은 다른 구조 |
| - | `policies/analyze/merge_priority.yml` | v5 multi-source 병합 전용 |
| - | `policies/analyze/manual_handoff_rules.yml` | confidence_rules에 통합 가능. 추후 검토 |
| - | `policies/analyze/spec_required_fields.yml` | people_spec.schema.json으로 대체 가능 |
| - | `policies/verify/*.yml` (6건) | self-check 상세 규칙. verify_result 구체화 시 반영 |
| - | `policies/framework/button_mapping.yml` | 유용하나 즉시 필요하지 않음 |
| - | `policies/framework/code_component_defaults.yml` | 유용하나 즉시 필요하지 않음 |

---

## 상충 규칙 및 처리 내용

| 상충 | 처리 |
|------|------|
| dual-spec vs single-spec | gdi_auto_work canonical = dual-spec (people_spec.md + machine_spec.yml) 고정 |
| popup 파라미터 규칙 | gdi_auto_work 기준 통일 (listener.modal/modalParam 패턴) |
| 화면유형 명칭 | 모든 v5 명칭을 TO-BE로 변환 완료 (grid-only→list 등) |
| 로컬 절대경로 | 복사 금지. v5의 `user/inbox/tasks/` → `work/task/1.Prep/` 변환 |
| v5 taskId 인자 구조 | gdi_auto_work는 active-context 기반 무인자 구조. command 재작성 완료 |

---

## 정합성 검증 결과

| 검증 항목 | 결과 |
|-----------|------|
| analyze 정책 파일 존재 | OK — ppt_extraction.yml, classify_tags.yml, screen_type_rules.yml |
| people_spec.md 템플릿 dual-spec 구조 | OK — YAML frontmatter + 11섹션 Markdown |
| screen_type 분류 결과명 TO-BE 일치 | OK — list/list-detail/form/popup 통일 |
| template_selection ↔ screen-types 일치 | OK — 4개 유형 skeleton 경로 일치 |
| skeleton_contract ↔ skeleton placeholder | OK — artifact_structure_contract 추가, 기존 placeholder 유지 |
| archive 입력 금지 유지 | OK — allowed_paths, runtime, commands 모두 반영 |
| CommonController 우선 원칙 | OK — CG-001~CG-003이 skeleton_contract에 통합 |

---

## 미확정 항목

| 항목 | 상태 |
|------|------|
| v5 `policies/verify/*.yml` (6건) | 보류 — verify_result self-check 구체화 시 반영 |
| v5 `button_mapping.yml` | 보류 — 버튼 매핑 자동화 필요 시 반영 |
| v5 `code_component_defaults.yml` | 보류 — 코드 컴포넌트 기본값 자동화 필요 시 반영 |
| PPT 파싱 실제 구현 | 미구현 — ppt_extraction.yml 규칙은 정의됨, 실행 로직 필요 |

---

## 다음 단계 작업

1. **실제 테스트 실행**: 샘플 PPT로 /analyze → /build 전체 흐름 테스트
2. **verify 정책 이식**: v5의 `policies/verify/*.yml` 6건을 verify_result self-check에 반영
3. **button_mapping 이식**: 버튼 자동 매핑 규칙 반영
4. **code_component_defaults 이식**: 코드 컴포넌트 기본값 규칙 반영
5. **test 체계 도입 (phase 2)**: v5의 test.md 기반 테스트 command 추가
