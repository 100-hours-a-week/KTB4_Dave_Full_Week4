# 최근 인기글 기능 작업 컨텍스트

작성 기준일: 2026-08-02  
프로젝트: `KTB4_Dave_Full_Week4`  
기술 스택: Java 21, Spring Boot 4.0.6, Spring Data JPA, MySQL 8.x, Bucket4j와 무관

## 이 문서의 사용법

다른 기기에서 저장소를 연 뒤 이 파일을 Codex에 첨부하거나 내용을 그대로 전달한다. 새 작업을 시작할 때는 다음과 같이 요청하면 된다.

> `POPULAR_POST_HANDOFF.md`를 현재 작업 컨텍스트로 읽고, 실제 코드와 git diff를 다시 확인한 뒤 최근 인기글 기능 작업을 이어서 진행해줘. 기존 사용자 변경은 덮어쓰지 마.

이 문서는 설계와 현재 구현 상태를 전달하기 위한 문서다. 실제 작업 전에는 반드시 현재 브랜치의 코드와 `git status`, `git diff`를 다시 확인해야 한다.

## 기능 목표

- 인정된 게시글 조회수를 5분 단위 버킷에 누적한다.
- 5분마다 완료된 버킷만 사용해 최근 5분, 30분, 60분 조회수를 갱신한다.
- 요청마다 원본 버킷을 `SUM/GROUP BY`하지 않고 비정규화된 후보 테이블을 조회한다.
- 인기 점수 상위 10개 게시글을 제공한다.
- 삭제된 게시글과 신고 횟수 5회를 초과한 블라인드 게시글은 결과에서 제외한다.

## 인기 점수

```text
popularityScore = viewCount5m * 2 + viewCount30m + viewCount60m
```

누적 구간이 중첩되므로 실질적인 조회수 가중치는 다음과 같다.

| 조회 발생 시점 | 실질 가중치 |
|---|---:|
| 최근 5분 | 4 |
| 5~30분 전 | 2 |
| 30~60분 전 | 1 |

동점 정렬 기준은 다음과 같다.

1. `popularityScore DESC`
2. `viewCount5m DESC`
3. `viewCount30m DESC`
4. `postNum DESC`

## 시간과 버킷 경계

- 모든 계산은 `Instant`와 UTC 기반 `Clock`을 사용한다.
- 조회 기록 시각은 현재 시각을 5분 단위로 내린 버킷에 기록한다.
- 스케줄 집계 시 현재 작성 중인 버킷은 제외한다.
- 스케줄러는 5분 경계에서 10초 후 실행한다.

예를 들어 현재 시각이 `14:03`이면:

```text
현재 기록 버킷: [14:00, 14:05)
집계 종료 시각: 14:00
5분 집계:       [13:55, 14:00)
30분 집계:      [13:30, 14:00)
60분 집계:      [13:00, 14:00)
```

증분 갱신 시각은 다음과 같다.

```text
newBucketStart       = windowEndAt - 5분
expired30BucketStart = windowEndAt - 35분
expired60BucketStart = windowEndAt - 65분
```

## 데이터 구조

### `post_view_bucket`

5분 단위 원본 조회수다. `(post_num, bucket_start_at)`에 유니크 제약이 있으며 조회 시 native upsert로 `view_count`를 증가시킨다. upsert는 삭제되지 않고 신고 횟수가 5회 이하인 게시글에만 수행한다.

관련 파일:

- `src/main/java/com/example/community/post/entity/PostViewBucket.java`
- `src/main/java/com/example/community/post/repository/PostViewBucketRepository.java`

### `post_popularity_stat`

API 조회용 비정규화 테이블이다.

주요 컬럼:

```text
post_num
view_count5m
view_count30m
view_count60m
popularity_score
```

`view_count60m == 0`이 되면 후보에서 삭제한다. 누적 구간 관계상 60분 값이 0이면 5분과 30분 값도 0이어야 한다. `post_num`은 공유 기본 키이며 `Post`와 지연 로딩 `@OneToOne` 관계로 매핑한다. 전체 집계의 기준 시각은 통계 행마다 중복 저장하지 않고 `popularity_aggregation_checkpoint.last_processed_end_at`만 사용한다.

관련 파일:

- `src/main/java/com/example/community/post/entity/PostPopularityStat.java`
- `src/main/java/com/example/community/post/repository/PostPopularityStatRepository.java`

### `popularity_aggregation_checkpoint`

스케줄의 마지막 처리 완료 시각을 기록한다. 고정 작업명은 `POPULAR_POSTS`다. 스케줄 실행 시 체크포인트 행에 비관적 쓰기 락을 적용한다.

관련 파일:

- `src/main/java/com/example/community/post/entity/PopularityAggregationCheckpoint.java`
- `src/main/java/com/example/community/post/repository/PopularityAggregationCheckpointRepository.java`

## 주요 서비스 흐름

핵심 구현은 `src/main/java/com/example/community/post/service/PostViewService.java`에 있다.

### 조회수 기록

```text
PostService에서 24시간 중복 조회 판정
→ 실제 조회수 증가가 인정된 경우에만 PostViewService.recordView(postNum)
→ 현재 5분 버킷에 1 upsert
```

기존 `PostView.view()`는 조회수가 실제로 인정됐는지 알 수 있도록 `boolean`을 반환하도록 변경됐다.

현재 정책상 로그인한 사용자의 인정된 조회만 버킷에 기록된다. 익명 조회는 기존 `PostService.getPost()` 흐름대로 조회수 집계를 하지 않는다.

### 최초 집계

체크포인트의 `lastProcessedEndAt`이 `null`이면 완료된 최근 1시간 버킷을 읽어 후보 테이블을 한 번 재구축한다. 이후 체크포인트를 현재 완료 시각으로 이동한다.

### 증분 집계

정상 실행에서는 다음 세 시각의 버킷만 읽는다.

- 새롭게 완료된 5분 버킷
- 30분 집계에서 만료되는 버킷
- 60분 집계에서 만료되는 버킷

스케줄이 누락된 경우 체크포인트 다음 구간부터 현재 완료 구간까지 5분 단위로 순차 처리한다.

### 상위 10개 조회

`PostPopularityStatRepository.findPopularPostNums(Pageable)`가 비정규화 테이블을 인기 점수 순으로 정렬한다. 이 쿼리에서 다음 게시글을 제외한다.

```text
post.deletedAt IS NOT NULL
postState.reportCount > 5
```

`PostViewService.getTop10PopularPostNums()`가 `PageRequest.of(0, 10)`을 사용한다.

`PostRepository.findPostByPostNumIn()`의 `IN` 조회는 순서를 보장하지 않으므로 `PostService.getTop10PopularPosts()`에서 게시글을 Map으로 만든 뒤 인기순 ID 목록 기준으로 다시 배열한다. 최종 API 응답 순서는 인기 점수 순서가 유지된다.

API:

```http
GET /posts/popular
```

현재 이 API는 페이지 파라미터 없이 상위 10개만 반환한다.

## 스케줄러와 Clock

관련 파일:

- `src/main/java/com/example/community/configuration/SchedulingConfig.java`
- `src/main/java/com/example/community/post/scheduler/PopularPostScheduler.java`
- `src/main/resources/application.yaml`

설정:

```yaml
popular-post:
  scheduler:
    enabled: ${POPULAR_POST_SCHEDULER_ENABLED:true}
    cron: "10 */5 * * * *"
    zone: UTC
```

테스트 환경에서는 스케줄러 자동 실행을 비활성화한다.

```yaml
popular-post:
  scheduler:
    enabled: false
```

## DDL

`src/main/resources/db/mysql/schema.sql`에 다음 항목이 추가됐다.

- `post_view_bucket`
- `post_popularity_stat`
- `popularity_aggregation_checkpoint`
- 체크포인트 초기 행 `('POPULAR_POSTS', NULL)`

현재 운영 설정은 다음과 같다.

```yaml
spring.jpa.hibernate.ddl-auto: validate
spring.sql.init.mode: never
```

따라서 기존 EC2 MySQL에는 스키마가 자동으로 반영되지 않는다. 배포 전에 사용자가 별도의 증분 SQL을 작성해 세 테이블과 체크포인트 초기 행을 직접 적용해야 한다. 이전 요청에 따라 별도의 `add_post_view_bucket.sql` 파일은 저장소에서 삭제했다.

## 테스트

핵심 테스트:

- `src/test/java/com/example/community/post/service/PostViewServiceTest.java`
- `src/test/java/com/example/community/post/repository/PostPopularityStatRepositoryTest.java`

검증 항목:

- `Clock` 기준 5분 버킷 내림
- 현재 작성 중인 버킷을 제외한 최초 1시간 복원
- 신규 버킷 추가
- 30분 만료 버킷 차감
- 60분 만료 버킷 차감
- 인기 점수 계산
- 직전 5분 조회가 새 버킷에서 사라질 때 5분 값을 0으로 초기화
- 최근 조회 우선 동점 정렬
- 삭제·블라인드 게시글 제외
- `Pageable`을 통한 조회 결과 개수 제한

마지막 검증 결과:

```text
./gradlew test
BUILD SUCCESSFUL
```

## 현재 알려진 주의점과 후속 개선 후보

1. 스케줄은 5분 경계에서 10초 후 실행하지만, 그보다 늦게 커밋되는 과거 버킷 조회수는 증분 집계에서 누락될 수 있다. 운영 안정성을 높이려면 주기적인 최근 1시간 전체 보정 작업을 추가한다.
2. 체크포인트 비관적 락은 운영 DDL에 초기 행이 존재한다는 전제에서 다중 인스턴스 중복 실행을 방지한다. 기존 DB에 초기 행을 반드시 추가해야 한다.
3. `PostView` 엔티티 내부의 24시간 판정에는 아직 `Instant.now()`가 사용된다. 인기글 버킷과 스케줄 경계는 주입된 `Clock`을 사용하지만, 조회 중복 판정도 완전히 테스트 가능한 시간 구조로 만들려면 후속 리팩터링이 필요하다.
4. `post_view_bucket`은 원본 데이터이고 `post_popularity_stat`은 언제든 재생성 가능한 파생 데이터로 취급해야 한다.
5. 현재 작업 트리에는 인기글 기능 외에 사용자가 진행 중인 `Post`, `PostState`, `PostResponse`, `PostRepository` 관련 변경이 섞여 있다. 다른 기기에서 작업할 때 이 변경을 임의로 되돌리거나 덮어쓰면 안 된다.

## 다른 기기에서 시작할 때 확인할 명령

```bash
git status --short
git diff -- src/main/java/com/example/community/post
git diff -- src/main/resources/application.yaml
git diff -- src/main/resources/db/mysql/schema.sql
./gradlew test
```

Windows PowerShell에서는 `./gradlew` 대신 다음을 사용할 수 있다.

```powershell
.\gradlew.bat test
```
