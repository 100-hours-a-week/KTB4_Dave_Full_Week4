package com.example.community.user.service;

import com.example.community.post.dto.response.PostPageResponse;
import com.example.community.post.entity.Post;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserLikeRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.example.community.post.fixture.PostTestFixture.post;
import static com.example.community.post.fixture.PostTestFixture.user;


@ExtendWith(MockitoExtension.class)
class LikedPostQueryServiceTest {
    @Mock
    private UserLikeRepository userLikeRepository;

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    @InjectMocks
    private LikedPostQueryService likedPostQueryService;

    @Test
    @DisplayName("좋아요한 게시글은 작성자의 프로필 이미지 URL을 반환한다")
    void getMyLikePostsReturnsAuthorProfileImageUrl() {
        SignUserInfo signUserInfo = new SignUserInfo(
                1L,
                1L,
                UserRole.USER
        );
        UserInfo viewer = user(1L, "viewer", "profiles/viewer.png");
        UserInfo author = user(2L, "author", "profiles/author.png");
        Post post = post(10L, author);
        UserLikePost like = new UserLikePost(viewer, post);
        when(userLikeRepository.findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(like)));

        PostPageResponse response = likedPostQueryService.getMyLikePosts(
                signUserInfo,
                0,
                10,
                "latest"
        );

        assertThat(response.postTitleResponses())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.nickname()).isEqualTo("author");
                    assertThat(summary.profileImage()).isEqualTo(
                            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
                                    + "profiles/author.png"
                    );
                });
    }
























    @ParameterizedTest
    @CsvSource({
            "likes, post.postState.likeCount",
            "views, post.postState.viewCount"
    })
    @DisplayName("좋아요한 게시글은 요청한 기준과 게시글 번호 역순으로 정렬한다")
    void getMyLikePostsUsesRequestedSortAndPostNumAsTieBreaker(
            String sort,
            String primarySortProperty
    ) {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userLikeRepository.findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        likedPostQueryService.getMyLikePosts(signUserInfo, 0, 10, sort);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userLikeRepository).findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders = pageable.getSort().stream().toList();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo(primarySortProperty);
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(1).getProperty()).isEqualTo("post.postNum");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("좋아요한 게시글은 기본적으로 게시글 번호 역순으로 정렬한다")
    void getMyLikePostsUsesPostNumDescendingAsDefaultSort() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        when(userLikeRepository.findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        likedPostQueryService.getMyLikePosts(signUserInfo, 0, 10, "latest");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userLikeRepository).findByUserInfo_ProfileId(
                eq(signUserInfo.profileId()),
                pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getSort().stream().toList())
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getProperty()).isEqualTo("post.postNum");
                    assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
                });
    }

    @Test
    @DisplayName("좋아요한 게시글이 없으면 첫 페이지에 빈 결과를 반환한다")
    void getMyLikePostsReturnsEmptyFirstPage() {
        SignUserInfo signUserInfo = new SignUserInfo(1L, 1L, UserRole.USER);
        Pageable pageable = PageRequest.of(
                0,
                1,
                Sort.by(Sort.Direction.DESC, "post.postNum")
        );
        when(userLikeRepository.findByUserInfo_ProfileId(
                signUserInfo.profileId(),
                pageable
        )).thenReturn(Page.empty(pageable));

        PostPageResponse response = likedPostQueryService.getMyLikePosts(
                signUserInfo,
                0,
                1,
                "latest"
        );

        assertThat(response.postTitleResponses()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.pageSize()).isEqualTo(1);
        assertThat(response.postCount()).isZero();
        assertThat(response.totalCount()).isZero();
        assertThat(response.totalPage()).isZero();
    }


}
