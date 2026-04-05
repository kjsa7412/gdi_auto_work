# deploy skill

## 목적
build 또는 fix 결과물을 실제 프로젝트에 반영하기 위한 준비/패키징/반영 대상 정리를 수행한다.

**주의: 배포 환경이 아직 미확정일 수 있다. 외부 환경에 무조건 배포한다고 가정하지 않는다.**

## 입력

- `work/task/3.Result/deliverables/**` — build 결과 산출물
- 또는 `work/fix/3.Result/deliverables/**` — fix 결과 산출물
- target path 정보 (사용자 지정 또는 framework_manifest의 source_roots 참조)

## 수행 절차

### 1. Deploy 대상 확인
deliverables 경로에서 배포 대상 파일 목록을 수집한다.

### 2. Artifact 존재 검증
목록의 모든 파일이 실제 존재하는지 확인. 누락 파일이 있으면 경고.

### 3. 반영 대상 구조 정리
framework_manifest.yml의 source_roots를 기반으로 각 파일의 배포 위치를 결정:
- HTML → `{templates_root}/project/{module_group}/{sub_group}/`
- XML → `{mappers_root}/sjerp/{module_group}/{sub_group}/`
- Controller → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/`
- Service → `{java_root}/proj/{module_group}/{sub_group}/{module_id}/`

### 4. Overwrite 위험 경고
대상 경로에 이미 동일 이름 파일이 존재하면 사용자에게 경고한다. 덮어쓰기 전 확인 요청.

### 5. 배포 준비 결과 기록
```yaml
deploy_summary:
  total_files: 4
  target_paths:
    - source: "deliverables/DTA030.html"
      target: "templates/project/dt/dta/DTA030.html"
      status: "ready"
    - source: "deliverables/dta030.xml"
      target: "mapper/sjerp/dt/dta/dta030.xml"
      status: "ready"
  warnings: []
  conflicts: []
```

### 6. 실제 복사 (사용자 확인 후)
사용자가 확인하면 deliverables에서 target으로 파일 복사. 복사 결과 기록.

## 출력
- deploy summary
- copied/prepared paths
- warnings/conflicts

## 실패 조건
- deliverables 없음
- target path 미확정
- 충돌 해결 불가

## 중요
환경이 확정되지 않았으면 "배포 준비 완료. 대상 파일 목록:" 수준으로 안내하고 실제 복사는 수행하지 않는다.
