package com.example.community.post.service;

import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.*;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.community.post.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {
    private static final Instant WRITE_AT =
            Instant.parse("2026-08-01T00:00:00Z");
    private static final SignUserInfo SIGN_USER =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo ADMIN_USER =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    @Mock
    private PostDetailReadService postDetailReadService;

    @Mock
    private PostViewRecordingService postViewRecordingService;

    @Mock
    private PopularPostSnapshotService popularPostSnapshotService;

    @InjectMocks
    private PostQueryService postQueryService;

    @ParameterizedTest(name = "sort={0}")
    @CsvSource({
            "likes, postState.likeCount",
            "views, postState.viewCount"
    })
    @DisplayName("게시글 목록은 요청한 정렬 조건과 게시글 번호 역순을 적용한다")
    void getPostsByPageUsesRequestedSort(
            String sort,
            String primaryProperty
    ) {
        when(postRepository.findPostByPage(any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        postQueryService.getPostsByPage(
                2,
                20,
                sort
        );

        verify(postRepository).findPostByPage(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders = pageable.getSort().stream().toList();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo(primaryProperty);
        assertThat(orders.get(0).getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(1).getProperty()).isEqualTo("postNum");
        assertThat(orders.get(1).getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("게시글 목록은 기본적으로 게시글 번호 역순으로 정렬한다")
    void getPostsByPageUsesPostNumDescendingAsDefaultSort() {
        when(postRepository.findPostByPage(any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        postQueryService.getPostsByPage(0, 20, "latest");

        verify(postRepository).findPostByPage(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().stream().toList())
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getProperty()).isEqualTo("postNum");
                    assertThat(order.getDirection())
                            .isEqualTo(Sort.Direction.DESC);
                });
    }

    @Test
    @DisplayName("게시글 목록 Repository 결과를 응답으로 변환한다")
    void getPostsByPageReturnsRepositoryPage() {
        Post post = post(
                10L,
                user(1L, "author", "profiles/author.png")
        );
        when(postRepository.findPostByPage(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        PostPageResponse response = postQueryService.getPostsByPage(
                0,
                20,
                "latest"
        );

        assertThat(response.postTitleResponses())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.postNum()).isEqualTo(10L);
                    assertThat(summary.profileImage()).isEqualTo(
                            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
                                    + "profiles/author.png"
                    );
                });
    }


    @Test
    @DisplayName("로그인한 사용자가 작성한 게시글 목록을 조회한다")
    void getMyPostsReturnsSignedUserPosts() {
        Post post = post(10L, user(1L, "author"));
        Page<Post> page = new PageImpl<>(List.of(post));
        when(postRepository.findPostByUserInfo_ProfileId(
                eq(1L),
                any(Pageable.class)
        )).thenReturn(page);

        PostPageResponse response = postQueryService.getMyPosts(
                SIGN_USER,
                0,
                20,
                "views"
        );

        assertThat(response.postTitleResponses())
                .extracting(PostTitleResponse::postNum)
                .containsExactly(10L);
        verify(postRepository).findPostByUserInfo_ProfileId(
                eq(1L),
                any(Pageable.class)
        );
        verifyNoInteractions(userInfoRepository);
    }




    @Test
    @DisplayName("게시글이 없으면 일반 조회에 실패한다")
    void getPostThrowsWhenPostDoesNotExist() {
        when(postDetailReadService.read(10L))
                .thenThrow(new NotFoundException("존재하지 않는 게시글"));

        assertThatThrownBy(() -> postQueryService.getPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @Test
    @DisplayName("블라인드된 게시글은 일반 조회할 수 없다")
    void getPostThrowsWhenPostIsBlind() {
        when(postDetailReadService.read(10L)).thenReturn(detail(6));

        assertThatThrownBy(() -> postQueryService.getPost(SIGN_USER, 10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("신고 처리된 게시글");
        verifyNoInteractions(postViewRecordingService);
    }

    @Test
    @DisplayName("로그인 정보가 없으면 조회 이력을 기록하지 않고 게시글을 반환한다")
    void getPostReturnsPostForAnonymousUser() {
        when(postDetailReadService.read(10L)).thenReturn(detail(0));

        PostDetailResponse response = postQueryService.getPost(null, 10L);

        assertThat(response.postNum()).isEqualTo(10L);
        assertThat(response.objectKey()).isEqualTo("posts/detail.png");
        assertThat(response.profileImage()).isEqualTo(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
                        + "profiles/author.png"
        );
        verifyNoInteractions(postViewRecordingService);
    }

    @Test
    @DisplayName("프로필 번호가 없는 로그인 정보도 익명 조회로 처리한다")
    void getPostReturnsPostWhenProfileIdIsNull() {
        when(postDetailReadService.read(10L)).thenReturn(detail(0));
        SignUserInfo signUserInfo = new SignUserInfo(
                1L,
                null,
                UserRole.USER
        );

        PostDetailResponse response = postQueryService.getPost(signUserInfo, 10L);

        assertThat(response.postNum()).isEqualTo(10L);
        verifyNoInteractions(postViewRecordingService);
    }

    @Test
    @DisplayName("조회 사용자가 존재하지 않으면 게시글 조회에 실패한다")
    void getPostThrowsWhenViewerDoesNotExist() {
        when(postDetailReadService.read(10L)).thenReturn(detail(0));
        doThrow(new NotFoundException("존재하지 않는 유저"))
                .when(postViewRecordingService)
                .record(1L, 10L, WRITE_AT);

        assertThatThrownBy(() -> postQueryService.getPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("로그인 상세 조회는 조회 이력 처리를 전용 서비스에 위임한다")
    void getPostDelegatesViewRecordingForSignedUser() {
        when(postDetailReadService.read(10L)).thenReturn(detail(0));

        PostDetailResponse response = postQueryService.getPost(SIGN_USER, 10L);

        assertThat(response.viewCount()).isZero();
        verify(postViewRecordingService).record(1L, 10L, WRITE_AT);
    }































    @Test
    @DisplayName("인기글 통계에서 결정한 게시글 순서를 유지한다")
    void getTop10PopularPostsPreservesPopularityOrder() {
        when(popularPostSnapshotService.getSnapshot())
                .thenReturn(snapshot(3L, 2L, 1L));

        PostSliceResponse response = postQueryService.getTop10PopularPosts();

        assertThat(response.postTitleResponses())
                .extracting(PopularPostTitleResponse::postNum)
                .containsExactly(3L, 2L, 1L);
        assertThat(response.hasNext()).isFalse();
        verifyNoInteractions(postRepository);
    }

    @Test
    @DisplayName("인기 목록은 스냅샷의 안정 요약 필드만 반환한다")
    void getTop10PopularPostsUsesStableSnapshotFields() {
        when(popularPostSnapshotService.getSnapshot())
                .thenReturn(snapshot(3L, 1L));

        PostSliceResponse response = postQueryService.getTop10PopularPosts();

        assertThat(response.postTitleResponses())
                .extracting(PopularPostTitleResponse::postNum)
                .containsExactly(3L, 1L);
        assertThat(response.page()).isZero();
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.postCount()).isEqualTo(2);
    }





    private void givenPost(Post post) {
        when(postRepository.findByPostNum(post.getPostNum()))
                .thenReturn(Optional.of(post));
    }

}
