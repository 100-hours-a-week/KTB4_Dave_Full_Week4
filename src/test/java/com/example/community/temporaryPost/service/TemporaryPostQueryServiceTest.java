package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostTitleResponse;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import com.example.community.user.entity.UserRole;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.community.temporaryPost.fixture.TemporaryPostTestFixture.temporaryPost;
import static com.example.community.temporaryPost.fixture.TemporaryPostTestFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporaryPostQueryServiceTest {

    private static final long TEMPORARY_ID = 10L;
    private static final Instant WRITE_AT =
            Instant.parse("2026-08-05T00:00:00Z");
    private static final SignUserInfo OWNER =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo OTHER_USER =
            new SignUserInfo(2L, 2L, UserRole.USER);
    private static final SignUserInfo ADMIN =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private TemporaryPostRepository temporaryPostRepository;

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    private TemporaryPostAccess temporaryPostAccess;

    private TemporaryPostQueryService temporaryPostQueryService;

    @BeforeEach
    void setUpServices() {
        temporaryPostAccess = new TemporaryPostAccess(
                temporaryPostRepository
        );
        temporaryPostQueryService = new TemporaryPostQueryService(
                temporaryPostRepository,
                temporaryPostAccess,
                imageUrlBuilder
        );
    }



    @Test
    @DisplayName("소유자는 임시저장글을 조회할 수 있다")
    void getTemporaryPostReturnsPostForOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "title",
                "content",
                "posts/image.png"
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        TemporaryPostResponse response =
                temporaryPostQueryService.getTemporaryPost(OWNER, TEMPORARY_ID);

        assertThat(response.title()).isEqualTo("title");
        assertThat(response.content()).isEqualTo("content");
        assertThat(response.image()).endsWith("posts/image.png");
        assertThat(response.objectKey()).isEqualTo("posts/image.png");
        assertThat(response.writeAt().toInstant()).isEqualTo(WRITE_AT);
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글은 조회할 수 없다")
    void getTemporaryPostThrowsWhenTemporaryPostDoesNotExist() {
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> temporaryPostQueryService.getTemporaryPost(
                OWNER,
                TEMPORARY_ID
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 임시저장글");
    }

    @Test
    @DisplayName("일반 사용자는 다른 사용자의 임시저장글을 조회할 수 없다")
    void getTemporaryPostThrowsWhenUserIsNotOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "title",
                "content",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostQueryService.getTemporaryPost(
                OTHER_USER,
                TEMPORARY_ID
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근권한 부족");
    }

    @Test
    @DisplayName("관리자도 다른 사용자의 임시저장글을 조회할 수 없다")
    void getTemporaryPostThrowsWhenAdminIsNotOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "title",
                "content",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostQueryService.getTemporaryPost(
                ADMIN,
                TEMPORARY_ID
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근권한 부족");
    }

    @Test
    @DisplayName("로그인 사용자의 임시저장글 목록을 응답으로 변환한다")
    void getTemporaryPostsReturnsSignedUsersPosts() {
        TemporaryPost firstPost = temporaryPost(
                10L,
                user(1L, "owner"),
                "first",
                "content-1",
                null
        );
        TemporaryPost secondPost = temporaryPost(
                20L,
                firstPost.getUserInfo(),
                "second",
                "content-2",
                null
        );
        when(temporaryPostRepository.findByUserInfo_ProfileId(1L))
                .thenReturn(List.of(firstPost, secondPost));

        List<TemporaryPostTitleResponse> response =
                temporaryPostQueryService.getTemporaryPosts(OWNER);

        assertThat(response)
                .extracting(TemporaryPostTitleResponse::temporaryPostId)
                .containsExactly(10L, 20L);
        assertThat(response)
                .extracting(TemporaryPostTitleResponse::title)
                .containsExactly("first", "second");
        verify(temporaryPostRepository)
                .findByUserInfo_ProfileId(OWNER.profileId());
    }













}
