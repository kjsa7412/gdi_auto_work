# gdi_auto_work 시스템 사용 절차 및 가이드

## 1. 문서 목적

이 문서는 `kjsa7412/gdi_auto_work` 저장소를 기준으로, 시스템의 **운영 개념**, **실행 절차**, **명령별 역할**, **작업자가 따라야 할 실무 가이드**를 한 번에 이해할 수 있도록 정리한 운영 안내서다.

이 저장소는 단순한 코드 생성기가 아니라, **PPT 화면설계서를 분석하여 spec을 만들고, 사람 검토를 거쳐, skeleton 기반으로 산출물을 생성하는 통제형 워크플로 시스템**으로 설계되어 있다.

---

## 2. 시스템 한 줄 요약

- 입력: PPT 화면설계서
- 중간 산출물: `people_spec.md`, `machine_spec.yml`, manifest, verify 결과
- 최종 산출물: HTML/XML/Controller/Service 등 deliverables
- 운영 방식: `/analyze → 사용자 검토 → /build → 필요 시 /fix → /clean`
- 핵심 통제 원칙: **single-active workspace / dual-spec / cache-first / skeleton-only generation / manifest 기반 추적**

---

## 3. 이 시스템을 어떻게 이해해야 하는가

이 저장소를 보면 실제 애플리케이션 소스보다도, 다음 요소들이 중심이다.

1. `.claude/commands`  
   Claude Code에서 실행할 상위 command 정의

2. `.claude/skills`  
   build/fix 내부에서 호출되는 하위 skill 정의

3. `system/`  
   정책, 스키마, 템플릿, skeleton, cache, runtime 설정을 모아 둔 시스템 핵심 영역

4. `work/`  
   현재 실행 중인 task/fix의 실제 작업 공간

5. `archive/`  
   완료된 작업 이력 보관 공간

즉, 이 저장소는 “생성 결과물만 만드는 레포”가 아니라,
**작업 입력 → 분석 → 검토 → 생성 → 수정 → 정리** 전체 수명주기를 통제하는 운영형 시스템 레포로 보는 것이 맞다.

---

## 4. 핵심 설계 원칙

### 4.1 Skeleton 기반 생성

이 시스템은 자유롭게 코드를 쓰는 방식이 아니다.  
반드시 미리 정의된 skeleton과 placeholder 치환 방식으로만 코드를 만든다.

의미:
- 임의 구조 생성 금지
- 화면 유형별 정해진 패턴 사용
- 검증 가능한 코드 생성 강제

### 4.2 Dual-Spec 구조

사람이 보는 문서와 기계가 보는 문서를 분리한다.

- `people_spec.md`: 사람 검토용
- `machine_spec.yml`: build 입력용

의미:
- 사람이 내용을 수정하기 쉬움
- 사람이 적은 설명이 기계 입력을 오염시키지 않음
- build 시점에 최종 기계 스펙을 다시 확정 가능

### 4.3 Single-Active Workspace

동시에 하나의 task 또는 fix만 활성화한다.

의미:
- 여러 작업이 섞여 문맥 오염되는 것을 차단
- 상태 전이가 명확해짐
- 실패 시 clean 후 재시작하는 운영 모델이 쉬워짐

### 4.4 Cache-First

DB 코드/테이블/뷰/함수/트리거 정보는 우선 cache에서 읽고,
부족할 때만 DB fallback 조회를 허용한다.

의미:
- 반복 실행 시 속도/안정성 개선
- 근거를 파일로 남길 수 있음
- LLM 기억보다 외부 근거를 우선하는 구조

### 4.5 Manifest 기반 실행

각 command는 실행 결과를 manifest로 남긴다.

의미:
- 어떤 파일을 입력으로 썼는지 추적 가능
- 어떤 산출물이 나왔는지 확인 가능
- 실패 위치와 원인 기록 가능
- 다음 단계가 대화가 아니라 파일 상태로 이어짐

### 4.6 Archive는 기록 전용

`archive/`는 이력 보관소일 뿐, 실행 입력으로 쓰지 않는다.

의미:
- 이전 작업 오염 방지
- 기록과 실행을 분리

---

## 5. 전체 실행 절차

표준 흐름은 아래와 같다.

```text
/cache-refresh (선택 또는 최초 1회)
      ↓
/analyze
      ↓
사용자 검토(final/people_spec.md 수정)
      ↓
/build
      ↓
문제 있으면 /fix
      ↓
완료 후 /clean
```

실무적으로는 아래처럼 이해하면 된다.

1. 환경과 cache를 준비한다.
2. PPT를 `work/task/1.Prep/`에 넣는다.
3. `/analyze`로 spec 초안을 만든다.
4. 사람이 `final/people_spec.md`를 검토하고 보완한다.
5. `/build`로 최종 `machine_spec.yml`과 코드 산출물을 만든다.
6. 오류나 누락이 있으면 `work/fix/1.Prep/`에 fix 요청을 넣고 `/fix`를 수행한다.
7. 작업이 끝나면 `/clean`으로 현재 work를 archive로 넘기고 초기화한다.

---

## 6. 상태 전이 기준

이 시스템은 상태 기반으로 동작한다.

### 주요 상태

- `idle` : 초기 상태
- `task_prepared` : 입력 준비 상태
- `task_analyzed` : analyze 완료, 사용자 검토 대기
- `task_built` : build 완료
- `fix_applied` : fix 적용 완료
- `failed` : 실패 상태
- `cleaned` : clean 완료 후 사실상 idle과 동일

### 운영 해석

- `task_analyzed` 이전에는 build를 실행하면 안 된다.
- `task_built` 이전에는 fix를 실행하면 안 된다.
- `failed` 상태에서는 clean만 허용된다.
- clean 없이 같은 상태에서 억지 재시도하는 방식은 지양해야 한다.

---

## 7. 디렉토리 운영 가이드

## 7.1 최상위 구조

```text
gdi_auto_work/
├── .claude/
├── system/
├── work/
├── archive/
└── 각종 보고서/마이그레이션 문서
```

## 7.2 `.claude/`

실행 명령과 skill을 담는다.

- `commands/`: analyze, build, fix, clean, cache-refresh
- `skills/`: review, generate, deploy, system-fix 등
- `settings.json`: Claude 동작 관련 설정

### 운영 포인트
- command는 오케스트레이터 역할
- skill은 세부 처리 역할
- 실무상 command를 기준으로 쓰고, skill은 내부 로직으로 이해하면 된다

## 7.3 `system/`

이 시스템의 핵심 규칙과 골격이 들어 있다.

### 주요 하위 영역

- `config/`: 경로, runtime, framework manifest, naming
- `cache/convention/`: 코드/테이블/뷰/함수/트리거 cache
- `templates/`: skeleton, screen type, spec 템플릿
- `schemas/`: manifest/spec 검증용 schema
- `policies/`: analyze/framework/runtime/verify 정책

### 운영 포인트
- 실제 생성 품질은 `system/` 품질에 의해 좌우된다
- 결과물 문제를 반복 수정해야 한다면 `system/` 정책이나 skeleton을 먼저 의심해야 한다

## 7.4 `work/`

현재 활성 작업 공간이다.

### task 영역

- `work/task/1.Prep/` : PPT 입력
- `work/task/2.Working/` : 중간 산출물
- `work/task/3.Result/` : 최종 deliverables / report

### fix 영역

- `work/fix/1.Prep/` : fix 요청 입력
- `work/fix/2.Working/` : fix 중간 처리
- `work/fix/3.Result/` : fix 결과

### 운영 포인트
- task와 fix를 물리적으로 분리해 컨텍스트를 격리한다
- 사용자는 주로 `1.Prep`, `2.Working/final`, `3.Result`만 집중해서 보면 된다

## 7.5 `archive/`

clean 시점에 `work/` 전체가 timestamp 기반 디렉토리로 이동된다.

### 운영 포인트
- 기록 보관소다
- 현재 실행 입력으로 사용하면 안 된다
- 회고/비교/이력 추적용으로만 본다

---

## 8. command별 상세 가이드

## 8.1 `/cache-refresh`

### 역할
DB에서 convention cache를 최신 상태로 갱신하는 독립 command.

### 언제 쓰는가
- 최초 실행 전
- DB 구조나 공통코드가 변경된 후
- analyze 전에 cache를 강제로 정리하고 싶을 때

### 입력
- `system/cache/convention/sql/` 하위 SQL들

### 출력
- `code.txt`
- `table.txt`
- `view.txt`
- `function.txt`
- `trigger.txt`

### 주의점
- phase와 무관하게 실행 가능
- lock이 풀려 있어야 한다
- 0 byte 파일이 생기면 warning 대상이다

### 실무 권장
- 첫 실행 시 1회 수행
- DB 정의가 자주 바뀌는 프로젝트면 analyze 전마다 점검

---

## 8.2 `/analyze`

### 역할
PPT를 분석하여 다음 3개를 만든다.

- `original/people_spec.md`
- `original/machine_spec.yml`
- `final/people_spec.md`

### 입력
- `work/task/1.Prep/`의 `.ppt` 또는 `.pptx`

### 내부 동작 핵심
1. lock 획득
2. active context를 task/analyze 상태로 설정
3. convention cache refresh를 **무조건 1회 수행**
4. 필요 시 DB fallback으로 cache 보강
5. PPT 추출
6. 화면 후보 식별
7. screen type 분류
8. people_spec(original) 생성
9. machine_spec(original) 생성
10. people_spec(final) 복사
11. manifest 기록 후 `task_analyzed` 상태로 종료

### analyze 이후 사람이 해야 하는 일
가장 중요하다.

반드시 아래 파일을 검토한다.

```text
work/task/2.Working/final/people_spec.md
```

여기에서 확인/수정해야 할 것:
- 화면 개요
- 검색 조건
- 그리드 컬럼
- 버튼 액션
- 비즈니스 규칙
- 미확정 항목
- 실제 화면 의도와 다른 오해석 부분

### analyze 단계에서 기억할 점
- analyze는 코드를 생성하지 않는다
- analyze의 목적은 “사람이 검토할 수 있는 초안”을 만드는 데 있다
- unresolved가 많으면 build 전에 people_spec 보강이 중요하다

---

## 8.3 `/build`

### 역할
사용자가 수정한 `final/people_spec.md`를 반영해
최종 `final/machine_spec.yml`을 만들고,
그 스펙을 기준으로 skeleton 기반 코드를 생성한다.

### build 전 필수 조건
아래가 모두 있어야 한다.

- `original/people_spec.md`
- `original/machine_spec.yml`
- `final/people_spec.md`
- `analyze.manifest.yml`
- 현재 상태가 `task_analyzed`

### 내부 동작 핵심
1. lock 획득
2. `build.manifest.yml` 초기화
3. `review` skill 호출
4. `final/machine_spec.yml` 생성
5. unresolved 있으면 중단
6. cache 검증 및 필요 시 DB fallback
7. `generate` skill 호출
8. skeleton 기반 산출물 생성
9. `verify_result.yml` 생성
10. deliverables / report 정리
11. 조건 만족 시 `deploy` skill을 **prepare-only** 모드로 호출
12. `task_built` 상태로 종료

### build의 핵심 포인트

#### 1) build는 review를 포함한다
즉, 사용자가 machine_spec을 직접 수정하는 것이 아니라,
**people_spec 수정 → build 내부 review → machine_spec 최종 확정** 흐름이다.

#### 2) build 이후 기준 문서는 final machine spec이다
이후 단계는 아래 파일이 기준이다.

```text
work/task/2.Working/final/machine_spec.yml
```

#### 3) deploy는 기본적으로 준비만 한다
실제 복사는 하지 않는다.

즉,
- target path 계산
- overwrite/conflict 탐지
- deploy summary 생성
까지만 수행한다.

### build 결과로 확인할 파일

- `work/task/2.Working/final/machine_spec.yml`
- `work/task/2.Working/manifests/build.manifest.yml`
- `work/task/2.Working/manifests/verify_result.yml`
- `work/task/3.Result/deliverables/`
- `work/task/3.Result/report/build_report.md`
- `work/task/3.Result/report/deploy_summary.yml` (조건 충족 시)

### 실무 검토 포인트
- verify 결과가 pass/warn인지
- generated 파일 수가 예상과 맞는지
- deliverables에 HTML/XML만 있는지, Controller/Service도 필요한지
- deploy_summary에 target path 충돌이 없는지

---

## 8.4 `/fix`

### 역할
오류나 변경 요청을 분석하여 영향 범위를 판단하고,
패치/재생성/정책 보완 여부를 판정하는 오케스트레이터다.

### 진입 조건
- 상태가 `task_built`
- `work/fix/1.Prep/`에 fix 요청 파일 존재
- lock 해제 상태

### fix 요청 예시
- 생성된 컬럼명이 잘못되었다
- 특정 버튼 이벤트가 빠졌다
- skeleton 자체가 잘못되어 반복 오류가 난다
- 정책이 바뀌어서 analyze/build 규칙 수정이 필요하다

### 내부 동작 핵심
1. fix 요청 파일 분석
2. 오류 유형 분류
3. cache 및 DB fallback으로 원인 조회
4. 영향 범위 판정
5. verify 기준으로 오류 분류
6. escalation rule에 따라 `system-fix` skill 연계 여부 판단
7. 유형별 조치 수행
8. fix verify 결과와 manifest 생성
9. `fix_applied` 상태로 종료

### fix 유형 해석

- `output_patch` : 산출물 수준 수정
- `rebuild` : build 재실행 필요
- `reanalyze` : analyze부터 다시 해야 함
- `skeleton_patch` : skeleton 수정 필요
- `policy_patch` : 정책 수정 필요

### 실무 포인트
반복 오류가 발생하면 output patch로만 끝내지 말고,
반드시 skeleton 또는 policy 보정이 필요한지 확인해야 한다.

이 시스템의 fix는 단순 문서 수정이 아니라,
**재발 방지용 구조 보정 트리거**로 이해하는 것이 맞다.

---

## 8.5 `/clean`

### 역할
현재 `work/` 전체를 `archive/`로 이동하고,
다음 작업을 위한 빈 작업 공간으로 초기화한다.

### 언제 실행하는가
- 작업이 완료되었을 때
- failed 상태에서 재시작해야 할 때
- 다음 task로 넘어가기 전

### 내부 동작 핵심
1. lock 획득
2. `work/**` 전체를 `archive/work_{timestamp}/`로 이동
3. 빈 work 구조 재생성
4. active context 초기화
5. lock 해제
6. 상태를 idle로 전환

### 주의점
- 일부만 정리하는 방식은 허용되지 않는다
- archive 내 파일 삭제는 금지다
- failed 상태에서 빠져나오려면 clean이 사실상 필수다

---

## 9. 작업자 표준 운영 절차

아래 순서로 운용하면 된다.

## 9.1 새 작업 시작

1. 이전 작업이 남아 있지 않은지 확인
2. 필요하면 `/clean`
3. 필요하면 `/cache-refresh`
4. PPT를 `work/task/1.Prep/`에 배치
5. `/analyze`

## 9.2 분석 결과 검토

검토 대상:

```text
work/task/2.Working/final/people_spec.md
```

점검 항목:
- 화면 수가 맞는가
- 화면 유형 분류가 맞는가
- 필드/버튼/그리드 정의가 누락되지 않았는가
- 미확정 항목이 실제로 무엇인지 명확한가
- 사람이 원하는 정책이 충분히 반영되었는가

## 9.3 코드 생성

1. people_spec을 수정 완료
2. `/build` 실행
3. deliverables 및 verify_result 확인
4. deploy_summary 확인

## 9.4 수정 요청 반영

1. fix 요청 내용을 `work/fix/1.Prep/`에 배치
2. `/fix` 실행
3. fix 결과와 verify 결과 확인
4. 구조 문제면 system policy/skeleton 보완 여부 점검

## 9.5 종료

1. 결과물 백업/확인
2. `/clean`
3. 다음 작업 준비

---

## 10. 어떤 파일을 우선 봐야 하는가

실무 기준 우선순위는 아래와 같다.

### 작업자 입장
1. `README.md`
2. `.claude/commands/*.md`
3. `work/task/2.Working/final/people_spec.md`
4. `work/task/2.Working/manifests/*.yml`
5. `work/task/3.Result/report/*`

### 시스템 설계/보정 담당자 입장
1. `system/config/runtime.yml`
2. `system/config/paths.yml`
3. `system/policies/framework/*.yml`
4. `system/policies/analyze/*.yml`
5. `system/policies/runtime/*.yml`
6. `system/templates/skeletons/**`
7. `system/schemas/**`

---

## 11. 자주 발생할 수 있는 운영 이슈

## 11.1 build가 안 되는 경우

가능 원인:
- analyze를 안 했음
- `task_analyzed` 상태가 아님
- `final/people_spec.md` 또는 analyze.manifest가 없음
- review 단계 unresolved 존재
- skeleton 계약 위반

대응:
- analyze 산출물 존재 여부 확인
- people_spec의 미확정 항목 보강
- review 결과 diff/unresolved 확인

## 11.2 fix가 기대처럼 동작하지 않는 경우

가능 원인:
- task 결과물에 직접 수정이 필요하다고 오해함
- fix는 task 영역을 읽기 전용으로 참조함
- 실제 문제는 output patch가 아니라 policy/skeleton 문제임

대응:
- fix 유형을 먼저 분류
- 반복 오류면 system-fix 대상 여부 확인

## 11.3 이전 작업이 섞이는 경우

원칙상 섞이면 안 된다.

점검:
- clean이 제대로 되었는지
- archive를 입력으로 잘못 참조하지 않았는지
- lock/active context가 꼬이지 않았는지

## 11.4 DB 정보 불일치

가능 원인:
- cache 오래됨
- cache 부족
- cache와 현재 화면 요구가 충돌

대응:
- `/cache-refresh`
- analyze/build manifest의 cache 관련 필드 확인
- DB fallback이 발생했는지 확인

---

## 12. 이 시스템을 잘 쓰는 운영 원칙

### 원칙 1. analyze 결과를 그대로 믿지 말고 people_spec을 반드시 검토한다
이 시스템은 human-in-the-loop 구조다.  
사람 검토를 생략하면 build 품질이 떨어질 수밖에 없다.

### 원칙 2. machine_spec을 직접 손보기보다 people_spec 수정 후 build로 재확정하는 흐름을 우선한다
운영상 중심 입력은 사람 검토 결과다.

### 원칙 3. 반복 오류는 output이 아니라 skeleton/policy 문제로 본다
같은 문제가 반복되면 system 보정이 필요하다.

### 원칙 4. archive는 기록용이지 실행용이 아니다
재사용이 필요하면 archive에서 복사해서 새 Prep로 넣는 방식이 더 안전하다.

### 원칙 5. failed 상태에서는 clean 후 재시작하는 습관을 가진다
억지 재시도보다 상태 초기화가 안전하다.

---

## 13. 추천 운영 체크리스트

## 작업 시작 전
- [ ] lock 해제 상태인가
- [ ] active context가 idle 또는 적절한 시작 상태인가
- [ ] 이전 작업이 남아 있지 않은가
- [ ] 필요 시 cache-refresh 했는가
- [ ] PPT가 `work/task/1.Prep/`에 들어갔는가

## analyze 후
- [ ] 추출 화면 수가 맞는가
- [ ] screen type 분류가 타당한가
- [ ] `final/people_spec.md`를 검토했는가
- [ ] unresolved를 확인했는가

## build 후
- [ ] `final/machine_spec.yml` 생성됐는가
- [ ] verify_result가 pass/warn인가
- [ ] deliverables가 기대한 개수/형식인가
- [ ] deploy_summary의 충돌 여부를 확인했는가

## fix 후
- [ ] fix 유형이 정확히 분류됐는가
- [ ] 영향 범위가 적절히 판단됐는가
- [ ] 재발 방지 관점의 skeleton/policy 점검을 했는가

## 종료 시
- [ ] 결과를 확인했는가
- [ ] `/clean`으로 정리했는가

---

## 14. 실무용 권장 해석

이 저장소를 운영할 때는 다음처럼 역할을 나누면 효율적이다.

### 사용자/기획 검토자
- PPT 제공
- `final/people_spec.md` 검토 및 보완
- 생성 결과 확인

### 시스템 운영자
- command 실행
- manifest / verify 결과 확인
- clean / archive 관리

### 시스템 개선 담당자
- skeleton 보정
- policy 보정
- schema/manifest 계약 보완
- 반복 오류의 구조적 개선

---

## 15. 결론

`gdi_auto_work`는 단순 프롬프트 조합이 아니라,
**명령-정책-스키마-캐시-워크스페이스-아카이브**로 구성된 통제형 생성 시스템이다.

실행의 핵심은 다음 세 가지다.

1. **/analyze 후 사람 검토를 반드시 거친다**  
2. **/build는 skeleton 기반으로만 생성한다**  
3. **문제가 반복되면 /fix를 넘어 system policy/skeleton을 보정한다**

즉, 이 시스템은 “AI가 알아서 다 만드는 구조”가 아니라,
**사람 검토와 정책 통제를 통해 예측 가능하게 산출물을 만드는 구조**로 이해하고 운영해야 한다.

---

## 16. 분석 기준으로 참조한 저장소 파일

- `README.md`
- `.claude/commands/analyze.md`
- `.claude/commands/build.md`
- `.claude/commands/fix.md`
- `.claude/commands/cache-refresh.md`
- `.claude/commands/clean.md`
- `system/config/paths.yml`
- `system/config/runtime.yml`

