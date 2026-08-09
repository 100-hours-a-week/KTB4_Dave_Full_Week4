package com.example.community.post.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.PostLikeResponse;
import com.example.community.post.dto.response.PostReportResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostReport;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.repository.PostReportRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static com.example.community.post.fixture.PostTestFixture.post;
import static com.example.community.post.fixture.PostTestFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostInteractionServiceTest {
    private static final Instant WRITE_AT =
            Instant.parse("2026-08-01T00:00:00Z");
    private static final SignUserInfo SIGN_USER =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo ADMIN_USER =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private UserLikeRepository userLikeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PostInteractionService postInteractionService;
































    @Test
    @DisplayName("좋아요 이력이 없으면 좋아요를 추가한다")
    void likePostAddsLikeWhenItDoesNotExist() {
        UserInfo userInfo = user(1L, "viewer");
        Post post = post(10L, user(2L, "author"));
        givenUser(userInfo);
        givenPost(post);
        when(userLikeRepository
                .findByUserInfo_ProfileIdAndPost_PostNum(1L, 10L))
                .thenReturn(Optional.empty());

        PostLikeResponse response = postInteractionService.likePost(SIGN_USER, 10L);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(userLikeRepository).save(any(UserLikePost.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("좋아요 이력이 있으면 좋아요를 취소한다")
    void likePostRemovesExistingLike() {
        UserInfo userInfo = user(1L, "viewer");
        Post post = post(10L, user(2L, "author"));
        UserLikePost userLikePost = new UserLikePost(userInfo, post);
        givenUser(userInfo);
        givenPost(post);
        when(userLikeRepository
                .findByUserInfo_ProfileIdAndPost_PostNum(1L, 10L))
                .thenReturn(Optional.of(userLikePost));

        PostLikeResponse response = postInteractionService.likePost(SIGN_USER, 10L);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(userLikeRepository).delete(userLikePost);
    }

    @Test
    @DisplayName("좋아요 사용자가 없으면 처리에 실패한다")
    void likePostThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postInteractionService.likePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("좋아요 대상 게시글이 없으면 처리에 실패한다")
    void likePostThrowsWhenPostDoesNotExist() {
        givenUser(user(1L, "viewer"));
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postInteractionService.likePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("게시글 좋아요 여부를 Repository 결과대로 반환한다")
    void isLikePostReturnsLikeState(boolean liked) {
        UserInfo userInfo = user(1L, "viewer");
        Post post = post(10L, user(2L, "author"));
        givenUser(userInfo);
        givenPost(post);
        when(userLikeRepository
                .existsByUserInfo_ProfileIdAndPost_PostNum(1L, 10L))
                .thenReturn(liked);

        boolean result = postInteractionService.isLikePost(SIGN_USER, 10L);

        assertThat(result).isEqualTo(liked);
    }

    @Test
    @DisplayName("좋아요 여부 조회 사용자가 없으면 예외를 던진다")
    void isLikePostThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postInteractionService.isLikePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("좋아요 여부 조회 게시글이 없으면 예외를 던진다")
    void isLikePostThrowsWhenPostDoesNotExist() {
        givenUser(user(1L, "viewer"));
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postInteractionService.isLikePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @Test
    @DisplayName("다른 사용자의 게시글을 정상적으로 신고한다")
    void reportPostCreatesReport() {
        UserInfo reporter = user(1L, "reporter");
        Post post = post(10L, user(2L, "author"));
        givenPost(post);
        givenUser(reporter);
        when(postReportRepository
                .existsByPost_PostNumAndUserInfo_ProfileId(10L, 1L))
                .thenReturn(false);

        PostReportResponse response = postInteractionService.reportPost(
                SIGN_USER,
                10L
        );

        assertThat(response.blind()).isFalse();
        verify(postReportRepository).save(any(PostReport.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("여섯 번째 신고로 블라인드되면 게시글 제거 무효화를 발행한다")
    void reportPostPublishesRemovalWhenBlindThresholdIsReached() {
        UserInfo reporter = user(1L, "reporter");
        Post post = post(10L, user(2L, "author"));
        for (int count = 0; count < 5; count++) {
            post.report();
        }
        givenPost(post);
        givenUser(reporter);
        when(postReportRepository
                .existsByPost_PostNumAndUserInfo_ProfileId(10L, 1L))
                .thenReturn(false);

        PostReportResponse response = postInteractionService.reportPost(SIGN_USER, 10L);

        assertThat(response.blind()).isTrue();
        verify(eventPublisher).publishEvent(new PostChangedEvent.Removed(10L));
    }

    @Test
    @DisplayName("본인이 작성한 게시글은 신고할 수 없다")
    void reportPostThrowsWhenReportingOwnPost() {
        UserInfo userInfo = user(1L, "author");
        Post post = post(10L, userInfo);
        givenPost(post);
        givenUser(userInfo);

        assertThatThrownBy(() -> postInteractionService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("본인이 작성한 글은 신고할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 신고한 게시글은 다시 신고할 수 없다")
    void reportPostThrowsWhenReportIsDuplicated() {
        UserInfo reporter = user(1L, "reporter");
        Post post = post(10L, user(2L, "author"));
        givenPost(post);
        givenUser(reporter);
        when(postReportRepository
                .existsByPost_PostNumAndUserInfo_ProfileId(10L, 1L))
                .thenReturn(true);

        assertThatThrownBy(() -> postInteractionService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("이미 신고한 게시글입니다.");
    }

    @Test
    @DisplayName("신고 대상 게시글이 없으면 예외를 던진다")
    void reportPostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postInteractionService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @Test
    @DisplayName("신고 사용자가 없으면 예외를 던진다")
    void reportPostThrowsWhenUserDoesNotExist() {
        givenPost(post(10L, user(2L, "author")));
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postInteractionService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }







    private void givenPost(Post post) {
        when(postRepository.findByPostNum(post.getPostNum()))
                .thenReturn(Optional.of(post));
    }

    private void givenUser(UserInfo userInfo) {
        when(userInfoRepository.findByProfileId(userInfo.getProfileId()))
                .thenReturn(Optional.of(userInfo));
    }

}
