# 인기글 캐시 구현 플랜

## 1. 목표

현재 단일 백엔드 컨테이너의 제한된 메모리에서 최근 인기글 상위 10개에 대한 반복 조회 비용을 줄인다.

- 컨테이너 메모리 제한: `256m`
- JVM heap: `-Xms64m -Xmx128m`
- 인기글 갱신 주기: 5분
- 인기글 후보 기간: 게시글 작성 시각 기준 최근 72시간
- 캐시 대상: 인기 목록, 인기글 본문, 인기글 상태, 인기글 댓글 첫 페이지
- 캐시 저장 값: JPA 엔티티가 아닌 불변 DTO

캐시는 응답의 정확성을 결정하는 원본이 아니라 조회 최적화 계층으로만 사용한다. 캐시 판정에 실패하거나 게시글이 인기 목록에서 이탈한 경우 기존 DB 조회로 자연스럽게 폴백한다.

## 2. 확정된 설계

### API 경로

- 인기 목록은 기존 `GET /posts/popular`를 유지한다.
- 게시글 상세는 기존 `GET /posts/{postNum}`을 유지한다.
- 댓글 목록은 기존 `GET /comments/list/{postNum}?page=0&size=10`을 유지한다.
- 인기글 전용 상세 또는 댓글 API를 추가하지 않는다.

백엔드는 현재 메모리에 있는 인기 목록 스냅샷으로 캐시 적용 여부를 판단한다. 목록에서 이탈한 게시글도 일반 상세·댓글 조회로 정상 응답하므로 프론트 폴백이 필요 없다.

### JPA 모델

- `Post`와 `PostState`의 `@OneToOne` 및 공유 PK 구조는 유지한다.
- 기존 EntityGraph 조회는 쓰기·관리자·일반 목록 등 기존 사용처를 위해 유지한다.
- 캐시용 읽기 경로에만 본문 projection과 상태 projection을 추가한다.
- 캐시 미스와 비인기글 상세 조회 모두 본문과 상태를 항상 별도로 조회한다.

히트/부분 히트 조합마다 EntityGraph와 분리 쿼리를 선택하는 분기는 만들지 않는다. 동일한 DTO 로더를 사용하고, 인기 여부에 따라 캐시를 통과할지만 결정한다.

## 3. 캐시 데이터 구조

### 인기 목록 스냅샷

```java
record PopularPostSnapshot(
        List<PopularPostSummaryCacheValue> orderedPosts,
        Set<Long> postNums
) {
    boolean contains(long postNum) {
        return postNums.contains(postNum);
    }
}
```

`PopularPostSummaryCacheValue`에는 목록 화면에서 사용하는 안정 데이터만 저장한다.

```text
postNum
nickname
profileImage
title
writeAt
```

인기 목록 화면에서 사용하지 않는 `viewCount`, `likeCount`, `reportCount`, `commentCount`는 인기 목록 DTO와 캐시 값에서 제거한다.

### 본문 캐시 값

`PopularPostBodyCacheValue`는 다음 값을 보관한다.

```text
postNum
nickname
profileImage
title
content
image objectKey
editedAt
writeAt
```

이미지 URL처럼 실행 환경에 따라 조립되는 값은 가능하면 응답 조립 시 생성한다.

### 상태 캐시 값

`PopularPostStateCacheValue`는 다음 값을 보관한다.

```text
viewCount
likeCount
reportCount
commentCount
```

블라인드 여부는 `reportCount > 5`로 계산한다.

### 댓글 캐시 값

댓글 첫 페이지 전체 DTO를 한 엔트리로 저장하지 않고 두 단계로 분리한다.

```text
PopularCommentFirstPageIndex
  key: postNum
  value: 정렬된 commentNum 목록 + totalCount

PopularCommentCacheValue
  key: commentNum
  value: postNum, parentNum, 작성자, 내용,
         depth, childCount, 수정/삭제 여부, 작성 시각
```

첫 페이지 인덱스가 적중하더라도 개별 댓글 캐시에 누락된 ID가 있으면 누락 ID 전체를 `IN` 쿼리 한 번으로 조회한다. 댓글 수만큼 단건 쿼리를 실행하지 않는다.

첫 페이지 인덱스의 `page=0`, `pageSize=10`은 상수로 사용한다. 응답의 `commentCount`는 조립된 ID 목록 크기, `totalPage`는 `ceil(totalCount / 10)`으로 계산한다.

## 4. 인기 목록 갱신

목록의 정상 갱신은 TTL 만료가 아니라 5분 집계 완료 이벤트가 담당한다.

1. 인기글 통계 집계를 트랜잭션으로 완료한다.
2. 커밋 성공 후 상위 10개 ID와 안정 데이터를 조회한다.
3. 새 `PopularPostSnapshot`을 완성한다.
4. 기존 목록 캐시를 먼저 제거하지 않고 새 스냅샷으로 원자적으로 교체한다.
5. 이전 목록과 새 목록을 비교하여 개별 캐시를 정리한다.

목록 스냅샷 자체의 TTL은 15분으로 두며 정상 갱신 주기가 아닌 스케줄러 실패·누락에 대한 안전장치로 사용한다. 애플리케이션 시작 직후 스냅샷이 없을 때는 첫 요청이 cache-aside 방식으로 생성한다.

### 목록 교체 시 개별 캐시 처리

- 유지된 게시글: 이미 존재하는 본문·댓글 캐시만 touch하여 비활성 만료 시간을 연장한다.
- 새 인기글: 본문과 댓글을 미리 조회하지 않는다.
- 이탈한 게시글: 본문·상태·댓글 페이지 인덱스·연결된 개별 댓글 캐시를 제거한다.

본문과 댓글은 `expireAfterAccess=10분`과 `expireAfterWrite=30분`을 함께 사용한다. 5분 목록 갱신 때 유지된 인기글을 touch하면 10분 비활성 만료는 연장되지만, 최초 적재 또는 실제 재적재 후 30분이 지나면 반드시 DB에서 다시 조회한다.

후보 기간 72시간, 삭제 여부, 블라인드 여부는 인기 목록 생성 쿼리에서만 검사한다. 상세·댓글 요청에서는 후보 조건을 다시 조회하지 않고 스냅샷의 `postNums.contains(postNum)`만 사용한다.

## 5. 상세 조회 흐름

1. 현재 캐시의 인기 스냅샷을 확인한다.
2. `postNums`에 포함되면 본문 캐시와 상태 캐시를 각각 조회한다.
3. 포함되지 않으면 동일한 본문·상태 projection을 캐시 없이 각각 조회한다.
4. 상태의 `reportCount`로 블라인드 여부를 검사한다.
5. 인증 사용자라면 캐시 로더와 무관하게 조회수 기록을 수행한다.
6. 본문 DTO와 상태 DTO를 기존 `PostDetailResponse` 형태로 조립한다.

현재 `getPost()`가 EntityGraph로 `Post` 전체를 다시 읽어 조회수를 증가시키면 본문 캐시의 효과가 사라진다. 조회수 기록을 다음과 같이 분리한다.

- 본문 DTO의 `postNum`, `writeAt`을 조회수 기록에 전달한다.
- `PostView`의 24시간 중복 여부만 조회한다.
- 유효한 신규 조회이면 `PostStateRepository`의 증가 쿼리로 조회수를 올린다.
- 인기글 조회 버킷도 동일 트랜잭션에서 기록한다.
- 조회수 변경으로 상태 캐시를 제거하지 않는다.

상태 응답은 최대 1분 이전 값일 수 있으며 이는 프론트 낙관적 UI 정책으로 수용한다.

## 6. 댓글 조회 흐름

댓글 캐시는 다음 조건을 모두 만족할 때만 사용한다.

```text
현재 인기 목록에 포함
page == 0
size == 10
```

그 외 페이지와 비인기글 댓글은 기존 DB 페이지 조회를 사용한다.

인기글 첫 페이지 캐시 조회는 다음 순서로 동작한다.

1. `postNum`으로 첫 페이지 인덱스를 조회한다.
2. 인덱스 미스이면 기존 페이지 쿼리를 실행하고 인덱스와 개별 댓글 캐시를 함께 채운다.
3. 인덱스 히트이면 `commentNum`별 캐시를 확인한다.
4. 누락된 댓글은 ID 묶음으로 한 번에 조회해 채운다.
5. 인덱스 순서대로 `CommentPageResponse`를 조립한다.

삭제 댓글도 마스킹된 상태로 목록에 남으므로 개별 댓글 재조회 쿼리는 `deletedAt is null`로 삭제 댓글을 제외하지 않아야 한다.

## 7. 무효화 처리

무효화는 DB 트랜잭션이 실제 커밋된 후 실행한다. 서비스가 도메인 이벤트를 발행하고 `@TransactionalEventListener(phase = AFTER_COMMIT)`가 캐시를 정리한다. 롤백된 변경은 캐시에 영향을 주지 않는다.

개별 캐시는 해당 키가 없어도 무조건 제거할 수 있다. 인기 목록 스냅샷은 새로 로딩하지 않고 `getIfPresent()`로 현재 메모리에 있는 값만 검사한다.

```java
PopularPostSnapshot snapshot = popularPostSnapshotCache.getIfPresent();
if (snapshot != null && snapshot.contains(postNum)) {
    popularPostSnapshotCache.invalidate();
}
```

구체적인 이벤트별 정책은 `POPULAR_POST_CACHE_POLICY.md`를 기준으로 한다.

## 8. Caffeine 설정과 메모리 예산

가변 길이 `TEXT`를 저장하는 본문과 댓글은 엔트리 개수만으로 실제 메모리 사용량을 제한하기 어렵다. 문자열 길이와 고정 객체 비용을 보수적으로 계산하는 `Weigher`와 `maximumWeight`를 사용한다.

```text
본문 캐시 논리 예산: 약 4 MiB
개별 댓글 캐시 논리 예산: 약 8 MiB
목록·상태·댓글 인덱스: maximumSize 기반 소형 캐시
```

가중치는 실제 JVM 객체 크기를 정확히 측정한 값이 아니라 캐시 admission/eviction을 위한 근사치다. 모든 TTL·크기·가중치 값은 `popular-post.cache.*` 설정과 환경변수로 조정할 수 있게 한다.

초기 기본값은 다음과 같이 고정한다.

```yaml
popular-post:
  cache:
    enabled: true
    list-ttl: 15m
    body-idle-ttl: 10m
    body-max-ttl: 30m
    state-ttl: 1m
    comment-idle-ttl: 10m
    comment-max-ttl: 30m
    body-max-weight-bytes: 4194304
    comment-max-weight-bytes: 8388608
    state-max-size: 20
    comment-index-max-size: 20
```

운영 환경변수는 각각 `POPULAR_POST_CACHE_*` 형태로 연결한다.

동일 키에 대한 동시 콜드 미스는 `sync=true` 또는 Caffeine의 원자적 로더를 사용해 한 번만 DB 조회한다. 캐시 통계는 `recordStats`로 수집한다.

## 9. API 변경

`GET /posts/popular`의 외부 필드명과 페이지 메타데이터는 유지한다.

```json
{
  "code": "인기 글 불러오기 성공",
  "data": {
    "postTitleResponses": [
      {
        "postNum": 10,
        "nickname": "dave",
        "profileImage": null,
        "title": "제목",
        "writeAt": "2026-08-08T12:00:00+09:00"
      }
    ],
    "page": 0,
    "pageSize": 10,
    "postCount": 1,
    "hasNext": false
  }
}
```

인기 목록 항목에서 다음 필드만 제거한다.

```text
viewCount
likeCount
reportCount
commentCount
```

공개 게시글 응답에서는 신고 수를 노출하지 않는다.

- 공개 `PostTitleResponse`, `PostDetailResponse`, `PostResponse`: `reportCount`를 제거하고 `blind`를 제공한다.
- 공개 `PostReportResponse`: 신고 수 대신 현재 `blind` 여부를 제공한다.
- 인기 목록: 블라인드 게시글을 목록 생성 단계에서 제외하므로 `reportCount`와 `blind`를 모두 포함하지 않는다.
- 관리자 응답: 운영상 실제 신고 수가 필요하므로 관리자 전용 DTO로 `reportCount`를 유지한다.

블라인드 기준은 백엔드의 `reportCount > 5`이며 프론트가 신고 수로 다시 계산하지 않는다. 기존 상세·댓글·mutation API의 경로는 유지한다.

## 10. 테스트 및 완료 조건

### 캐시 설정

- FakeTicker로 목록 15분, 상태 1분, 본문·댓글의 10분 비활성/30분 강제 만료를 검증한다.
- 본문 4 MiB, 댓글 8 MiB 가중치 초과 시 eviction을 검증한다.
- 동일 키 동시 미스에서 로더가 한 번만 실행되는지 검증한다.

### 목록

- 5분 집계 성공 후 기존 값을 먼저 제거하지 않고 새 스냅샷으로 교체되는지 검증한다.
- 유지·신규·이탈 게시글의 touch/미선조회/제거 정책을 검증한다.
- 후보 기간과 블라인드 조건이 목록 생성에만 적용되는지 검증한다.

### 상세와 상태

- 인기글 콜드 미스에서 본문과 상태가 각각 한 번 조회되는지 검증한다.
- 전체 히트와 한쪽만 만료된 부분 미스를 검증한다.
- 비인기글은 캐시에 저장되지 않고 분리 projection으로 조회되는지 검증한다.
- 캐시 적중 여부와 무관하게 인증 사용자 조회수 기록이 수행되는지 검증한다.
- 조회·좋아요·댓글 변경 후 상태가 TTL 동안 유지되고 1분 뒤 갱신되는지 검증한다.
- 신고 수 6 도달 시 즉시 블라인드되고 관련 캐시가 커밋 후 제거되는지 검증한다.

### 댓글

- 인기글의 `page=0,size=10`만 캐시되는지 검증한다.
- 인덱스 콜드 미스가 페이지 인덱스와 개별 댓글을 함께 채우는지 검증한다.
- 일부 댓글 미스가 단일 `IN` 쿼리로 복구되는지 검증한다.
- 댓글 수정·삭제는 개별 댓글만, 루트 댓글 추가는 페이지 인덱스만 제거하는지 검증한다.
- 답글 변경 시 부모 댓글의 `childCount` 캐시가 제거되는지 검증한다.

### 트랜잭션과 API

- 커밋 성공 시에만 무효화되고 롤백 시 캐시가 유지되는지 검증한다.
- 인기 목록에서 상태 필드가 제거되고 나머지 JSON 계약이 유지되는지 검증한다.
- 전체 Gradle 테스트와 캐시 관련 repository/service/controller 테스트를 통과시킨다.

## 11. 후속 확장

현재는 인기글이 최근 조회 기반으로 계산된 명확한 hot set이므로 비인기글은 캐시하지 않는다. 운영 후 비인기글 상세의 재조회율, 캐시 hit rate, DB 지연, eviction 수, heap 사용량을 측정하고 반복 조회가 확인될 때 별도의 일반 hot-post 캐시를 검토한다.

Redis로 전환할 때도 인기 스냅샷 membership은 매우 작으므로 인스턴스별 L1에 유지할 수 있다. 인스턴스별 스냅샷 차이는 응답 정확도가 아니라 캐시 적중 여부에만 영향을 주며, 비인기 판정 시 DB 조회로 폴백한다.
