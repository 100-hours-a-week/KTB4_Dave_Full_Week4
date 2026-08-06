# objectKey 기반 이미지 등록·수정 API 명세

## 변경 목적

기존 수정 API는 `imageAction=KEEP|CHANGE`로 이미지 처리 방식을 전달했습니다. 변경 후에는 응답에서 받은 `objectKey`와 새 이미지 파일의 존재 여부만으로 이미지 상태를 결정합니다.

- 프론트는 기존 이미지를 다시 `MultipartFile`로 전송하지 않습니다.
- 백엔드는 유지 요청의 `objectKey`가 현재 리소스의 key인지 검증합니다.
- 게시글 작성에서 `temporaryPostId`와 해당 임시글의 `objectKey`를 함께 보내면 새 S3 객체를 생성하지 않습니다.
- 이미지 표시에는 URL 필드를 사용하고, `objectKey`는 이후 등록·수정 요청을 위한 값으로만 사용합니다.
- 별도의 회원정보 조회 API는 추가하지 않습니다.

## 변경 전·후 요약

| 구분 | 변경 전 | 변경 후 |
|---|---|---|
| 기존 이미지 유지 | `imageAction=KEEP` | 응답에서 받은 `objectKey` 전송 |
| 새 이미지로 교체 | `imageAction=CHANGE`와 파일 전송 | 파일만 전송 |
| 기존 이미지 삭제 | `imageAction=CHANGE`, 파일 생략 | `objectKey`와 파일 모두 생략 |
| 게시글 작성 시 임시 이미지 재사용 | 지원하지 않음 | `temporaryPostId`와 해당 임시글의 `objectKey` 전송 |
| key와 파일 동시 전송 | `KEEP`일 때만 오류 | 항상 `400 Bad Request` |
| key 검증 | 별도 검증 없음 | 현재 DB key 또는 본인 임시글 key인지 검증 |

빈 문자열 `objectKey`와 크기가 0인 파일은 전송하지 않은 것으로 처리합니다.

## 공통 이미지 처리 규칙

### 게시글·임시글·프로필 수정

| `objectKey` | 이미지 파일 | 처리 결과 |
|---|---|---|
| 현재 응답에서 받은 key | 없음 | 기존 이미지 유지 |
| 없음 | 비어 있지 않은 파일 | 파일 업로드 후 새 key로 교체 |
| 없음 | 없음 또는 빈 파일 | 기존 이미지 제거 |
| 현재 값과 다른 key | 없음 | `400 Bad Request` |
| 있음 | 비어 있지 않은 파일 | `400 Bad Request` |

수정 요청의 `objectKey`는 수정 대상 게시글, 임시글 또는 사용자에 현재 저장된 key와 정확히 일치해야 합니다. S3 객체 존재 여부가 아니라 DB에 저장된 현재 key를 기준으로 검증합니다.

### 게시글 작성

| `temporaryPostId` | `objectKey` | `image` 파일 | 처리 결과 |
|---|---|---|---|
| 없음 | 없음 | 비어 있지 않은 파일 | 임시저장 없이 새 파일을 업로드하여 작성 |
| 없음 | 없음 | 없음 | 임시저장 없이 이미지 없이 작성 |
| 본인 임시글 ID | 해당 임시글의 현재 key | 없음 | 임시저장 이미지를 재사용하여 작성 |
| 본인 임시글 ID | 없음 | 비어 있지 않은 파일 | 임시글을 기반으로 새 이미지를 업로드하여 작성 |
| 본인 임시글 ID | 없음 | 없음 | 임시글을 기반으로 이미지 없이 작성 |
| 없음 | 있음 | 없음 | `400 Bad Request` |
| 다른 사용자의 ID 또는 없는 ID | 선택적 | 선택적 | `400 Bad Request` |
| 본인 임시글 ID | 현재 값과 다른 key | 없음 | `400 Bad Request` |
| 선택적 | 있음 | 비어 있지 않은 파일 | `400 Bad Request` |

`temporaryPostId`는 선택값이므로 임시저장 없이 바로 게시글을 작성할 수 있습니다. 다만 `objectKey`를 재사용할 때는 반드시 `temporaryPostId`도 보내야 합니다. 백엔드는 ID로 로그인 사용자의 임시글을 조회한 뒤 해당 임시글에 현재 저장된 key와 요청의 `objectKey`가 정확히 일치하는지 검증합니다.

## 응답 변경

`objectKey`는 프론트가 이후 요청에 다시 사용해야 하는 응답에만 포함합니다. 게시글 목록, 댓글 목록, 인기 게시글, 좋아요 게시글 목록, 임시글 제목 목록에는 추가하지 않습니다.

### 첫 임시저장

`POST /temporaryPost`

변경 전:

```json
{
  "data": {
    "temporaryKeyId": 15
  }
}
```

변경 후:

```json
{
  "data": {
    "temporaryKeyId": 15,
    "objectKey": "posts/550e8400-e29b-41d4-a716-446655440000.jpg"
  }
}
```

이미지가 없으면 `objectKey`는 `null`입니다. `temporaryKeyId`는 이후 `PUT /temporaryPost/{temporaryId}` 호출을 위해 계속 보관해야 합니다.

### 이후 임시저장 및 임시글 상세 조회

- `PUT /temporaryPost/{temporaryId}`
- `GET /temporaryPost/{temporaryId}`

변경 전에는 `image`가 objectKey였습니다. 변경 후 `image`는 조회 URL이고 `objectKey`가 별도 필드로 제공됩니다.

```json
{
  "data": {
    "title": "임시 제목",
    "content": "임시 내용",
    "image": "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/posts/550e8400-e29b-41d4-a716-446655440000.jpg",
    "objectKey": "posts/550e8400-e29b-41d4-a716-446655440000.jpg",
    "writeAt": "2026-08-05T18:00:00+09:00"
  }
}
```

### 게시글 상세 조회

`GET /posts/{postNum}`

기존 응답에 `objectKey`가 추가됩니다.

```json
{
  "data": {
    "postNum": 10,
    "image": "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/posts/550e8400-e29b-41d4-a716-446655440000.jpg",
    "objectKey": "posts/550e8400-e29b-41d4-a716-446655440000.jpg"
  }
}
```

`POST /posts` 등록 응답과 `PATCH /posts/{postNum}` 수정 응답에는 `objectKey`를 추가하지 않습니다. 프론트는 성공 후 이동한 게시글 상세 조회 응답에서 key를 받습니다.

### 로그인 성공

`POST /users/state`

변경 전에는 `profileImage`가 objectKey였습니다. 변경 후 `profileImage`는 조회 URL이고 `objectKey`가 별도 필드로 제공됩니다.

```json
{
  "data": {
    "nickname": "dave",
    "profileImage": "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/profiles/550e8400-e29b-41d4-a716-446655440000.jpg",
    "objectKey": "profiles/550e8400-e29b-41d4-a716-446655440000.jpg",
    "accessToken": "..."
  }
}
```

별도의 회원정보 조회 API는 추가하지 않습니다. 프로필 수정에 사용할 초기 `objectKey`는 로그인 성공 응답에서 보관합니다.

### 회원정보 수정

`PATCH /users/info`

변경 전에는 `profileImage`가 objectKey였습니다. 변경 후 `profileImage`는 조회 URL이고 `objectKey`가 별도 필드로 제공됩니다.

```json
{
  "data": {
    "nickname": "dave2",
    "profileImage": "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/profiles/new-image.jpg",
    "objectKey": "profiles/new-image.jpg"
  }
}
```

새 프로필 이미지를 업로드한 경우 응답의 새 `objectKey`로 프론트 상태를 갱신해야 합니다.

## 요청 변경

요청은 모두 `multipart/form-data`입니다. `imageAction` 필드는 제거됩니다.

### 첫 임시저장

`POST /temporaryPost`

| 필드 | 형식 | 필수 | 설명 |
|---|---|---|---|
| `title` | string | 예 | 제목 |
| `content` | string | 예 | 본문 |
| `image` | file | 아니오 | 처음 저장할 이미지 |

첫 임시저장 요청에는 `objectKey`를 보내지 않습니다.

### 이후 임시저장

`PUT /temporaryPost/{temporaryId}`

| 필드 | 형식 | 필수 | 설명 |
|---|---|---|---|
| `title` | string | 예 | 제목 |
| `content` | string | 예 | 본문 |
| `objectKey` | string | 조건부 | 기존 이미지 유지 시 전송 |
| `image` | file | 조건부 | 새 이미지 선택 시 전송 |

### 게시글 작성

`POST /posts`

| 필드 | 형식 | 필수 | 설명 |
|---|---|---|---|
| `title` | string | 예 | 제목 |
| `content` | string | 예 | 본문 |
| `temporaryPostId` | number | 아니오 | 특정 임시글을 기반으로 발행할 때 전송 |
| `objectKey` | string | 조건부 | 해당 임시글 이미지 재사용 시 `temporaryPostId`와 함께 전송 |
| `image` | file | 조건부 | 새 이미지로 작성할 때 전송 |

임시저장 없이 바로 작성할 때는 `temporaryPostId`와 `objectKey`를 모두 생략합니다.

### 게시글 수정

`PATCH /posts/{postNum}`

| 필드 | 형식 | 필수 | 설명 |
|---|---|---|---|
| `title` | string | 예 | 제목 |
| `content` | string | 예 | 본문 |
| `objectKey` | string | 조건부 | 기존 이미지 유지 시 전송 |
| `image` | file | 조건부 | 새 이미지 선택 시 전송 |

### 회원정보 수정

`PATCH /users/info`

| 필드 | 형식 | 필수 | 설명 |
|---|---|---|---|
| `nickname` | string | 예 | 닉네임 |
| `objectKey` | string | 조건부 | 기존 프로필 이미지 유지 시 전송 |
| `imageFile` | file | 조건부 | 새 프로필 이미지 선택 시 전송 |

게시글과 임시글의 파일 필드명은 `image`, 프로필의 파일 필드명은 `imageFile`입니다.

## 프론트 변경사항

### 1. 응답 상태 분리

프론트 상태에 이미지 URL과 objectKey를 따로 저장합니다. URL에서 key를 잘라내거나 조립하지 않습니다.

```javascript
const imageState = {
  previewUrl: response.image,
  objectKey: response.objectKey,
  selectedFile: null,
};
```

프로필은 `response.profileImage`를 표시 URL로 사용하고 `response.objectKey`를 수정 요청용으로 보관합니다.

### 2. `imageAction` 제거

기존 코드의 다음 로직을 제거합니다.

```javascript
formData.append("imageAction", "KEEP");
formData.append("imageAction", "CHANGE");
```

### 3. 이미지 상태별 FormData 생성

```javascript
function appendPostImage(formData, { objectKey, selectedFile }) {
  if (selectedFile) {
    formData.append("image", selectedFile);
    return;
  }

  if (objectKey) {
    formData.append("objectKey", objectKey);
  }
}
```

- 기존 이미지 유지: `objectKey`만 전송
- 새 이미지 선택: 파일만 전송하고 기존 `objectKey`는 전송하지 않음
- 이미지 삭제: 둘 다 전송하지 않음

프로필 수정은 동일한 방식으로 파일 필드명만 `imageFile`을 사용합니다.

임시글을 게시글로 발행할 때는 첫 임시저장 응답의 `temporaryKeyId` 또는 현재 편집 중인 임시글 ID를 게시글 작성 요청의 `temporaryPostId`로 전송합니다.

```javascript
formData.append("temporaryPostId", temporaryKeyId);
appendPostImage(formData, { objectKey, selectedFile });
```

임시저장 없이 바로 게시글을 작성하면 `temporaryPostId`를 추가하지 않습니다.

### 4. 응답별 key 갱신

- 첫 임시저장 성공: `temporaryKeyId`와 `objectKey`를 저장
- 이후 임시저장 성공: 응답의 최신 `objectKey`로 교체
- 임시글 편집 진입: 임시글 상세 응답의 `objectKey` 사용
- 게시글 수정 진입: 게시글 상세 응답의 `objectKey` 사용
- 게시글 등록·수정 성공: 상세 페이지로 이동한 후 상세 응답의 `objectKey` 사용
- 로그인 성공: 프로필 `objectKey` 저장
- 회원정보 수정 성공: 응답의 최신 `objectKey`로 교체

이미지 삭제 성공 후 응답의 `objectKey`가 `null`이면 프론트에 보관한 기존 key도 제거합니다.

## 오류 응답

다음 요청은 `400 Bad Request`입니다.

- `objectKey`와 비어 있지 않은 파일을 동시에 전송
- `temporaryPostId` 없이 `objectKey` 전송
- 존재하지 않거나 로그인 사용자 소유가 아닌 `temporaryPostId` 전송
- 게시글·임시글·프로필 수정에서 현재 DB 값과 다른 `objectKey` 전송
- 게시글 작성에서 `temporaryPostId`의 현재 이미지와 다른 `objectKey` 전송
- 기존 제목, 본문 또는 닉네임 검증 조건 위반

## 배포 시 주의사항

기존 프론트는 `imageAction`을 전송하고 `objectKey`를 보관하지 않으므로 새 백엔드와 호환되지 않습니다. 또한 새 프론트는 기존 백엔드가 요구하는 `imageAction`을 보내지 않으므로 양쪽 변경은 함께 배포해야 합니다.

교체 또는 삭제된 기존 S3 객체를 실제로 제거하는 정책은 이번 변경 범위에 포함하지 않습니다. 데이터베이스에 저장되는 참조 key만 변경됩니다.
