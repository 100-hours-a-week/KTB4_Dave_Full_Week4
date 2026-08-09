# 로컬 부하 테스트

운영 배포와 분리된 일회용 부하 테스트 환경이다. 루트 배포 스크립트는
수정하지 않으며, `.dockerignore`가 이 디렉터리를 애플리케이션 이미지에서
제외한다. 프론트엔드, Grafana, Prometheus는 실행하지 않는다.

## 비교 기준

동일한 `ComparisonId`로 아래 세 환경을 순서대로 실행한다.

| 구분 | 브랜치 | 캐시 | 비교 의미 |
| --- | --- | --- | --- |
| A | `main` | 캐시 코드 없음 | 기존 기준선 |
| B | `develop` | OFF | 리팩터링/쿼리 변경 기준선 |
| C | `develop` | ON | 캐시 적용 결과 |

- B → C: 같은 코드와 커밋에서 캐시 설정만 바뀌므로 캐시의 순효과다.
- A → B: 캐시 외에 포함된 리팩터링과 쿼리 변경의 영향이다.
- A → C: `main` 대비 최종 변경 전체의 효과다.

`lifecycle.ps1`은 A를 `main`, B/C를 `develop`에서만 시작할 수 있게 하고,
B와 C가 서로 다른 커밋이면 결과 집계를 거부한다. 부하 테스트 설정 커밋은
두 브랜치에 동일하게 적용한 뒤 비교해야 한다. 실행기는 테스트 스크립트와
seed 파일의 해시도 비교해 서로 다르면 중단한다. 결과 디렉터리는 Git에서
제외되므로 브랜치를 전환해도 로컬에 계속 누적된다.

## 실행 환경

- Spring Boot: 1 CPU, 컨테이너 메모리 256 MiB
- JVM: `-Xms64m -Xmx128m`
- MySQL: 2 CPU, 컨테이너 메모리 2 GiB
- k6 OSS: Windows 호스트에서 실행
- S3: 더미 자격 증명만 사용하며 이미지 필드는 전부 비움

MySQL에 여유를 둔 이유는 외부 RDS 자체가 아니라 작은 EC2 애플리케이션의
단기 처리 한계를 보는 것이기 때문이다. 따라서 이 결과에는 실제 RDS의
네트워크 지연과 인스턴스 한계가 반영되지 않는다.

## 준비

1. Docker Desktop에 최소 3 CPU와 3 GiB 메모리를 할당하고 실행한다.
2. 로컬 실행용 k6 OSS를 설치한다. Windows에서는 다음 명령을 사용할 수 있다.

   ```powershell
   winget install k6 --source winget
   k6 version
   ```

3. PowerShell이 로컬 스크립트 실행을 차단하는 환경에서는 현재 터미널에만
   실행을 허용한다.

   ```powershell
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
   ```

4. 환경 파일을 복사하고 RDS 콘솔에 표시된 정확한 MySQL 패치 버전을 넣는다.

   ```powershell
   Copy-Item load-test/.env.example load-test/.env
   ```

운영 DB, JWT, AWS 자격 증명은 `.env`에 넣지 않는다.

## A/B/C 전체 실행

예시는 `cache-v1`이라는 하나의 비교 ID를 사용한다. 각 `up`은 현재 코드를
빌드하고 애플리케이션과 새 MySQL을 자동으로 실행한다. 프론트엔드는 실행하지
않는다.

### A: main, 기존 기준선

```powershell
git switch main
.\load-test\scripts\lifecycle.ps1 -Action up -Variant A
.\load-test\scripts\run-suite.ps1 -ComparisonId cache-v1 -Suite All
.\load-test\scripts\lifecycle.ps1 -Action down
```

### B: develop, 캐시 OFF

```powershell
git switch develop
.\load-test\scripts\lifecycle.ps1 -Action up -Variant B
.\load-test\scripts\run-suite.ps1 -ComparisonId cache-v1 -Suite All
.\load-test\scripts\lifecycle.ps1 -Action down
```

### C: 같은 develop 커밋, 캐시 ON

B 실행 뒤 코드나 커밋을 바꾸지 않는다.

```powershell
.\load-test\scripts\lifecycle.ps1 -Action up -Variant C
.\load-test\scripts\run-suite.ps1 -ComparisonId cache-v1 -Suite All
.\load-test\scripts\lifecycle.ps1 -Action down
```

`up`은 작업 트리에 미커밋 변경이 있으면 중단한다. `-AllowDirty`는 설정 확인용
진단 실행에만 사용할 수 있으며, 이 상태의 결과로는 A/B/C 비교 CSV를 만들지
않는다.

`down`은 `ktb-perf-*` 프로젝트 이름을 검증한 뒤 컨테이너와 MySQL 볼륨을
제거한다. 테스트 데이터는 API soft delete를 거치지 않고 일회용 DB 볼륨과
함께 물리적으로 삭제된다.

## 실행 범위 조절

기본 부하는 `1, 2, 5, 10, 20, 40, 80, 160 RPS`, 각 단계 45초다. 오류/체크
임계값을 넘은 단계는 한 번 더 시도하고, 두 번 실패하면 해당 시나리오의
상승을 멈춘다.

```powershell
# 캐시 관련 세 경로만 실행
.\load-test\scripts\run-suite.ps1 `
  -ComparisonId cache-v1 `
  -Suite Cache `
  -Rps 1,5,10,20,40 `
  -Duration 30s

# 단일 시나리오 실행
.\load-test\scripts\run-k6.ps1 `
  -ComparisonId cache-v1 `
  -Scenario posts-list `
  -Rps 1,5,10
```

전체 시나리오는 `login`, `posts-list`, `post-detail`, `popular-posts`,
`comment-list`, `post-create`, `comment-create`, `post-like`다.

캐시 전용 시나리오는 다음 세 경로만 측정한다.

- `popular-posts`: 인기 게시글 목록 스냅샷
- `post-detail-popular`: 인기 상위 10개 게시글의 본문/상태
- `comment-list-popular`: 인기 상위 10개 게시글의 첫 댓글 페이지(0, 10)

각 시나리오는 측정 전에 1 RPS로 15초간 워밍업한다. 따라서 캐시 비교는
콜드 스타트가 아니라 워밍된 정상 상태를 대상으로 한다.

## 결과 파일

모든 파일은 `load-test/results/<ComparisonId>/` 아래에 남고 Git에는 포함되지
않는다.

```text
results/cache-v1/
├─ raw/<A|B|C>/<scenario>/*.json
├─ resources/<A|B|C>/<scenario>/*.csv
├─ metadata/*.json
├─ overall.csv
├─ cache-only.csv
└─ cache-abc-comparison.csv
```

- `raw`: 단계별 k6 원본 summary JSON. 내장 지표와 사용자 정의 지표를 보존한다.
- `resources`: 같은 단계의 애플리케이션 컨테이너 CPU/메모리/IO 1초 샘플이다.
- `overall.csv`: 모든 시나리오와 성공/실패 시도의 요청 수, 처리량, 평균,
  median, p90/p95/p99, 최대 지연, 오류율, dropped iteration을 누적한다.
- `cache-only.csv`: 캐시 대상 세 경로의 동일 지표만 따로 누적한다. 실패한
  단계도 원본 기록으로 남는다.
- `cache-abc-comparison.csv`: 통과한 단계만 RPS와 경로별로 A/B/C를 나란히
  놓고 B → C 캐시 개선율과 A → C 전체 개선율을 계산한다.

개선율은 `(이전 지연 - 이후 지연) / 이전 지연 × 100`이다. 양수면 빨라졌고,
음수면 느려진 것이다. 고정 arrival rate 테스트이므로 p95/p99뿐 아니라
`achieved_rps`, `error_rate`, `dropped_iterations`도 같이 판단한다.

Grafana나 Prometheus 없이도 위 JSON/CSV로 전후 결과를 기록하고 비교할 수
있다. 실제 Caffeine hit/miss 비율까지 필요할 때만 애플리케이션 내부 메트릭
노출과 시계열 수집기를 추가한다.
