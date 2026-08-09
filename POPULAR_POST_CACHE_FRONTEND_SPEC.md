# 인기글 캐시 적용 프론트 변경 명세

## 1. 변경 요약

캐시는 백엔드 내부 동작이므로 인기글 상세와 댓글 API 경로는 변경하지 않는다. 프론트 변경은 인기 목록 응답 타입에서 사용하지 않는 상태 필드를 제거하고, 공개 게시글 응답의 `reportCount` 기반 블라인드 판정을 `blind` 사용으로 변경하며, mutation 성공 값을 낙관적 UI에 유지하는 작업이 중심이다.

## 2. 변경되는 API

### `GET /posts/popular`

경로와 응답 envelope, 페이지 메타데이터는 유지한다.

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

인기 목록 항목에서 다음 필드가 제거된다.

```text
viewCount
likeCount
reportCount
commentCount
```

프론트의 인기 목록 응답 타입을 상태 필드가 없는 전용 타입으로 변경한다.

## 3. 공개 게시글의 블라인드 계약 변경

사용자에게 실제 신고 수를 노출하지 않는다. 공개 게시글 관련 응답의 `reportCount`를 제거하고 백엔드가 계산한 `blind`를 제공한다.

```json
{
  "blind": true
}
```

적용 대상은 공개 `PostTitleResponse`, `PostDetailResponse`, `PostResponse`, `PostReportResponse`다. 관리자 API는 운영을 위해 실제 `reportCount`를 유지한다.

- 프론트에서 `reportCount > 5`를 계산하는 코드를 제거한다.
- 목록·상세 표시 여부는 응답의 `blind`를 사용한다.
- 백엔드의 마스킹과 상세 접근 차단을 최종 기준으로 취급한다.
- 인기 목록은 블라인드 게시글을 제외하므로 인기 목록 항목에는 `blind`가 없다.
- 신고 성공 응답도 누적 신고 수가 아니라 `blind`를 반환한다.

### 최종 프론트 타입 예시

아래 타입을 기준으로 기존 공개 게시글 타입에서 `reportCount`를 완전히 제거한다.

```ts
type PopularPostListItem = {
  postNum: number;
  nickname: string;
  profileImage: string | null;
  title: string;
  writeAt: string;
};

type PublicPostListItem = {
  postNum: number;
  nickname: string;
  profileImage: string | null;
  title: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  blind: boolean;
  writeAt: string;
};

type PublicPostResponse = {
  postNum: number;
  nickname: string;
  profileImage: string | null;
  title: string;
  content: string;
  image: string | null;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  blind: boolean;
  isEdited: boolean;
  writeAt: string;
};

type PublicPostDetailResponse = PublicPostResponse & {
  objectKey: string | null;
};

type PostReportResponse = {
  blind: boolean;
};
```

관리자 `/admin/posts` 응답은 공개 타입을 재사용하지 않는다. 관리자 목록과 상세 응답에는 기존 `reportCount: number`가 유지되고 `blind`는 추가되지 않는다.

### 엔드포인트별 적용 타입

| 엔드포인트 | 적용 타입 | 변경 사항 |
|---|---|---|
| `GET /posts/popular` | `PopularPostListItem` | 상태 필드 4개 및 `blind` 없음 |
| `GET /posts` | `PublicPostListItem` | `reportCount` 제거, `blind` 추가 |
| `GET /posts/my` | `PublicPostListItem` | `reportCount` 제거, `blind` 추가 |
| `GET /users/myLike` | `PublicPostListItem` | `reportCount` 제거, `blind` 추가 |
| `GET /posts/{postNum}` | `PublicPostDetailResponse` | `reportCount` 제거, `blind` 추가 |
| `POST /posts` | `PublicPostResponse` | `reportCount` 제거, `blind` 추가 |
| `PATCH /posts/{postNum}` | `PublicPostResponse` | `reportCount` 제거, `blind` 추가 |
| `POST /posts/{postNum}/report` | `PostReportResponse` | 누적 수 대신 `blind` 반환 |
| `/admin/posts/**` | 관리자 전용 타입 | 실제 `reportCount` 유지 |

## 4. 변경되지 않는 API

### 인기글 상세

인기 목록에서 게시글을 클릭해도 기존 상세 API를 그대로 사용한다.

```http
GET /posts/{postNum}
```

다음 항목을 추가하지 않는다.

- 인기글 전용 상세 경로
- `source=popular` 쿼리
- 인기글 여부를 나타내는 요청 헤더
- 인기글 이탈 시 별도 폴백 요청

백엔드가 현재 인기 목록을 기준으로 캐시 사용 여부를 결정하며, 목록에서 이탈한 게시글도 동일 상세 API에서 정상 조회한다.

### 댓글 첫 페이지

기존 요청을 유지한다.

```http
GET /comments/list/{postNum}?page=0&size=10
```

인기글 여부와 댓글 캐시 사용 여부는 백엔드 내부에서 결정한다. 두 번째 페이지부터도 기존 경로와 파라미터를 그대로 사용한다.

## 5. 상태 값과 낙관적 UI

인기글 상세의 상태 값은 백엔드에서 최대 1분 캐시된다.

```text
viewCount
likeCount
reportCount
commentCount
```

위 목록의 `reportCount`는 캐시 내부 상태를 의미하며 공개 상세 응답에는 포함되지 않는다. 공개 응답은 `blind`만 제공한다.

좋아요·댓글·신고 mutation 성공 후 상세를 즉시 다시 조회하면 캐시된 이전 수치가 반환될 수 있다. 다음 정책을 적용한다.

- 좋아요 성공 응답의 `likeCount`, `liked`를 로컬 상세 상태에 반영한다.
- 댓글 등록 성공 응답의 `numberOfComments`와 새 댓글을 로컬 상태에 반영한다.
- 댓글 삭제 성공 시 로컬 댓글 상태와 댓글 수를 먼저 반영한다.
- 신고 성공 응답의 `blind`를 로컬 상태에 반영하고 `true`이면 블라인드 UI 또는 접근 제한 흐름을 적용한다.
- mutation 확인만을 목적으로 상세 API를 즉시 재호출하지 않는다.
- mutation 이후 1분 이내의 상세 응답이 더 작은 수치를 반환하더라도 방금 성공한 mutation 상태를 덮어쓰지 않는다.

댓글 내용과 댓글 첫 페이지 데이터는 변경 커밋 후 해당 댓글 또는 인덱스 캐시가 제거되므로 필요한 경우 댓글 API는 다시 호출할 수 있다. 게시글의 `commentCount`만 상태 캐시 정책에 따라 최대 1분 지연될 수 있다.

## 6. 인기 목록 갱신 동작

- 백엔드는 약 5분 단위로 인기 목록 스냅샷을 교체한다.
- 프론트가 이미 표시 중인 목록의 게시글이 서버의 다음 목록에서 빠질 수 있다.
- 해당 게시글을 클릭하더라도 기존 상세 API가 일반 DB 조회로 폴백하므로 별도 오류 처리나 재시도는 필요 없다.
- 기존 프론트 목록 재조회 주기가 있다면 그대로 유지한다.

## 7. 오류 처리

- 인기 목록 이탈은 404 사유가 아니다.
- 삭제된 게시글은 기존 삭제 게시글 오류 정책을 따른다.
- 신고 6회로 블라인드된 게시글은 기존 상세 접근 제한 응답을 따른다.
- 캐시 hit/miss는 HTTP 상태나 응답 형태에 영향을 주지 않는다.

## 8. 배포 호환성

인기 목록에서 제거되는 네 상태 필드는 현재 화면에서 사용하지 않는다는 전제다. 공개 API의 `reportCount` 제거는 프론트 블라인드 판정 변경과 함께 배포해야 한다.

- 기존 프론트와 새 백엔드: `reportCount` 기반 블라인드 판정이 동작하지 않으므로 호환되지 않음
- 새 프론트와 기존 백엔드: `blind`가 없으므로 호환되지 않음
- API 경로: 모두 유지
- 댓글 응답: 기존 계약 유지
- 공개 게시글·신고 응답: `reportCount`에서 `blind`로 계약 변경

따라서 공개 블라인드 계약 변경은 백엔드와 프론트를 함께 배포한다. TypeScript 등 정적 타입 정의도 동시에 갱신한다.

## 9. 프론트 완료 체크리스트

- 인기 목록 전용 항목 타입에서 상태 필드 네 개 제거
- 인기글 클릭 시 기존 `/posts/{postNum}` 호출 유지
- 댓글 첫 요청의 `page=0,size=10` 유지
- 공개 게시글 타입에서 `reportCount` 제거 및 `blind` 추가
- 프론트의 `reportCount > 5` 계산 제거
- 신고 성공 응답을 `blind` 기준으로 처리
- 좋아요·댓글·신고 mutation 응답을 로컬 상태에 반영
- mutation 직후 상세 자동 재검증으로 이전 값이 덮어써지지 않도록 처리
- 인기 목록 이탈에 대한 별도 404 폴백 로직을 추가하지 않음
