# v5 → gdi_auto_work 미반영 자산 및 후속 작업

## 아직 미반영된 자산

| Source 파일 | 사유 | 우선순위 |
|------------|------|---------|
| `policies/verify/html_checks.yml` | verify_result self-check 구체화 시 반영 | 2순위 |
| `policies/verify/xml_checks.yml` | 상동 | 2순위 |
| `policies/verify/sql_checks.yml` | 상동 | 2순위 |
| `policies/verify/controller_checks.yml` | 상동 | 2순위 |
| `policies/verify/service_checks.yml` | 상동 | 2순위 |
| `policies/verify/escalation_rules.yml` | 상동 | 2순위 |
| `policies/framework/button_mapping.yml` | 버튼 매핑 자동화 시 반영 | 2순위 |
| `policies/framework/code_component_defaults.yml` | 코드 컴포넌트 기본값 자동화 시 | 2순위 |
| `policies/analyze/default_resolution.yml` | 미확정 항목 자동 해소 규칙 | 3순위 |
| `policies/analyze/merge_priority.yml` | 다중 소스 병합 규칙 | 3순위 |
| `policies/analyze/manual_handoff_rules.yml` | 수작업 전환 규칙 (confidence_rules에 부분 통합) | 3순위 |
| `policies/analyze/spec_required_fields.yml` | schema로 대체 가능 | 3순위 |
| `policies/analyze/source_access.yml` | v5 경로 체계 전용 | 재작성 필요 |

## 보류 자산

| Source 파일 | 사유 |
|------------|------|
| `.claude/commands/test.md` | test 체계는 phase 2 |
| Playwright 관련 전체 | 실행 검증은 phase 2 |
| `proposals/policy_changes/*` | system-fix skill 활성화 시 |
| `docs/templates/examples/SEA010_spec.md` | 실제 spec 예시. 참고용으로 보관 가능 |

## 후속 단계에서 해야 할 병합/재작성 작업

### 즉시 (다음 이터레이션)
- [ ] verify 정책 6건 이식 → generate skill의 self-check 항목 구체화
- [ ] button_mapping.yml → 버튼 자동 분류/매핑 로직에 반영
- [ ] code_component_defaults.yml → 코드 타입별 기본 설정 자동화

### 중기
- [ ] test command 도입 (v5 test.md 참고)
- [ ] proposals 폴더 구조 확립 (system-fix skill 연계)
- [ ] default_resolution.yml → analyze 미확정 항목 자동 해소 로직

### 장기
- [ ] Playwright 통합
- [ ] 자동 fix 생성 루프 (v5 fix.md의 auto-rebuild 패턴)
