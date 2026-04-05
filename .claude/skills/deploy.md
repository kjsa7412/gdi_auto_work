# deploy skill

## 목적
build 또는 fix 결과물을 실제 프로젝트에 반영하기 위한 준비/패키징/반영 대상 정리를 수행한다.

**주의: 배포 환경이 아직 미확정일 수 있다. 외부 환경에 무조건 배포한다고 가정하지 않는다.**

## 동작 모드

### prepare-only (기본)
- build에서 호출될 때의 기본 모드
- deliverables 검증 → target path 계산 → conflict 탐지 → summary 생성
- **실제 파일 복사는 수행하지 않음**

### apply
- 사용자가 명시적으로 확인한 경우에만 수행
- prepare-only 결과를 기반으로 실제 파일 복사 실행
- 복사 결과 기록

## 입력

- `work/task/3.Result/deliverables/**` — build 결과 산출물
- 또는 `work/fix/3.Result/deliverables/**` — fix 결과 산출물
- `system/config/framework_manifest.yml` — source_roots 참조
- target path 정보 (사용자 지정 또는 framework_manifest의 source_roots 참조)
- `mode`: "prepare-only" (기본) 또는 "apply"

## 수행 절차

### 1. Deploy 대상 확인
deliverables 경로에서 배포 대상 파일 목록을 수집한다.

### 2. Artifact 존재 검증
목록의 모든 파일이 실제 존재하는지 확인. 누락 파일이 있으면 경고.

### 3. 반영 대상 구조 정리 (Target Path 계산)
framework_manifest.yml의 source_roots와 supported_artifacts를 기반으로 각 파일의 배포 위치를 결정:
- HTML → `{templates_root}/project/{module_group}/{sub_group}/{MODULE_ID}.html`
- XML → `{mappers_root}/sjerp/{module_group}/{sub_group}/{module_id}.xml`
- Controller → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/{ModuleId}Controller.java`
- Service → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/{ModuleId}Service.java`

target path를 계산할 수 없는 경우 해당 파일은 `status: "path_unknown"`으로 기록.

### 4. Overwrite/Conflict 검토
대상 경로에 이미 동일 이름 파일이 존재하면:
- prepare-only: conflict로 기록, 경고 메시지 생성
- apply: 사용자에게 덮어쓰기 확인 요청. 미확인 시 해당 파일 복사 건너뜀.

### 5. Deploy Summary 생성
`work/task/3.Result/report/deploy_summary.yml` (또는 fix 경로):
```yaml
deploy_summary:
  mode: "prepare-only"
  status: "prepared"  # prepared | warn | failed
  total_files: 4
  target_paths:
    - source: "deliverables/DTA030.html"
      target: "src/main/resources/templates/project/dt/dta/DTA030.html"
      status: "ready"       # ready | conflict | path_unknown | copied | skipped
    - source: "deliverables/dta030.xml"
      target: "src/main/resources/mapper/sjerp/dt/dta/dta030.xml"
      status: "ready"
  warnings: []
  conflicts: []
  copied_paths: []          # apply 모드에서만 채워짐
```

### 6. 실제 복사 (apply 모드, 사용자 확인 후)
apply 모드이고 사용자가 확인한 경우에만:
1. conflict가 없는 파일 또는 사용자가 덮어쓰기를 승인한 파일만 복사
2. 복사된 파일을 `copied_paths[]`에 기록
3. 복사 실패 시 해당 파일 status를 `failed`로 기록

## 출력

```yaml
deploy_result:
  mode: "prepare-only" | "apply"
  status: "prepared" | "warn" | "failed" | "applied" | "partial"
  total_files: N
  target_paths: [...]
  copied_paths: [...]       # apply 모드에서만
  warnings: [...]
  conflicts: [...]
```

## 실패 조건
- deliverables 경로에 파일이 없음
- target path를 하나도 계산할 수 없음
- framework_manifest.yml 누락 또는 source_roots 미정의

## 중요
- **build에서 호출 시 기본 모드는 prepare-only**이다. 실제 복사 없이 summary만 생성한다.
- 환경이 확정되지 않았으면 "배포 준비 완료. 대상 파일 목록:" 수준으로 안내하고 실제 복사는 수행하지 않는다.
- archive는 deploy 입력으로 사용 금지이다.
- prepare-only 결과는 build.manifest/report에 바로 붙일 수 있는 구조여야 한다.
- deploy는 독립적으로도 호출 가능하며, fix 결과물에 대해서도 동일하게 동작한다.
