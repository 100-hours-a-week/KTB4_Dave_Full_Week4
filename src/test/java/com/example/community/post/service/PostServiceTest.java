package com.example.community.post.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.DuplicateException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.response.*;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.entity.PostReport;
import com.example.community.post.entity.PostView;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostReportRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostViewRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.user.repository.UserLikeRepository;
import com.example.community.util.ImageConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
    private static final Instant WRITE_AT =
            Instant.parse("2026-08-01T00:00:00Z");
    private static final SignUserInfo SIGN_USER =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo ADMIN_USER =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostEditRepository postEditRepository;

    @Mock
    private PostViewRepository postViewRepository;

    @Mock
    private PostReportRepository postReportRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private UserLikeRepository userLikeRepository;

    @Mock
    private ImageConverter imageConverter;

    @Mock
    private PostViewService postViewService;

    @InjectMocks
    private PostService postService;

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

        postService.getPostsByPage(
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

        postService.getPostsByPage(0, 20, "latest");

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
        Post post = post(10L, user(1L, "author"));
        when(postRepository.findPostByPage(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        PostPageResponse response = postService.getPostsByPage(
                0,
                20,
                "latest"
        );

        assertThat(response.postTitleResponses())
                .extracting(PostTitleResponse::postNum)
                .containsExactly(10L);
    }

    @Test
    @DisplayName("관리자 게시글 목록은 블라인드된 제목도 원문으로 반환한다")
    void adminGetPostsByPageReturnsOriginalTitle() {
        Post post = post(10L, user(1L, "author"));
        blind(post);
        Page<Post> page = new PageImpl<>(List.of(post));
        when(postRepository.findPostByPage(any(Pageable.class)))
                .thenReturn(page);

        PostPageResponse response = postService.adminGetPostsByPage(
                0,
                20,
                "latest"
        );

        assertThat(response.postTitleResponses())
                .singleElement()
                .extracting(PostTitleResponse::title)
                .isEqualTo("title-10");
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

        PostPageResponse response = postService.getMyPosts(
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
    @DisplayName("게시글 수정 이력 목록을 반환한다")
    void getPostEditsByPageReturnsPage() {
        Post post = post(10L, user(1L, "author"));
        PostEditRecord editRecord = editRecord(5L, post);
        Page<PostEditRecord> page = new PageImpl<>(List.of(editRecord));
        when(postEditRepository.findByPost_PostNumOrderByEditIdDesc(
                eq(10L),
                any(Pageable.class)
        )).thenReturn(page);

        PostEditPageResponse response = postService.getPostEditsByPage(
                10L,
                0,
                20
        );

        assertThat(response.postEditTitleResponses())
                .extracting(PostEditTitleResponse::editId)
                .containsExactly(5L);
    }

    @Test
    @DisplayName("존재하는 게시글 수정 이력을 반환한다")
    void getPostEditReturnsExistingEdit() {
        PostEditRecord editRecord = editRecord(
                5L,
                post(10L, user(1L, "author"))
        );
        when(postEditRepository.findById(5L))
                .thenReturn(Optional.of(editRecord));

        PostEditResponse response = postService.getPostEdit(5L);

        assertThat(response.title()).isEqualTo("old-title");
        assertThat(response.content()).isEqualTo("old-content");
    }

    @Test
    @DisplayName("수정 이력이 없으면 예외를 던진다")
    void getPostEditThrowsWhenEditDoesNotExist() {
        when(postEditRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostEdit(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 수정 이력");
    }

    @Test
    @DisplayName("게시글이 없으면 일반 조회에 실패한다")
    void getPostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @Test
    @DisplayName("블라인드된 게시글은 일반 조회할 수 없다")
    void getPostThrowsWhenPostIsBlind() {
        Post post = post(10L, user(2L, "author"));
        blind(post);
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.getPost(SIGN_USER, 10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("신고 처리된 게시글");
        verifyNoInteractions(postViewRepository);
    }

    @Test
    @DisplayName("로그인 정보가 없으면 조회 이력을 기록하지 않고 게시글을 반환한다")
    void getPostReturnsPostForAnonymousUser() {
        Post post = post(10L, user(2L, "author"));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        PostResponse response = postService.getPost(null, 10L);

        assertThat(response.postNum()).isEqualTo(10L);
        assertThat(post.getPostState().getViewCount()).isZero();
        verifyNoInteractions(postViewRepository, postViewService);
    }

    @Test
    @DisplayName("프로필 번호가 없는 로그인 정보도 익명 조회로 처리한다")
    void getPostReturnsPostWhenProfileIdIsNull() {
        Post post = post(10L, user(2L, "author"));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        SignUserInfo signUserInfo = new SignUserInfo(
                1L,
                null,
                UserRole.USER
        );

        PostResponse response = postService.getPost(signUserInfo, 10L);

        assertThat(response.postNum()).isEqualTo(10L);
        verifyNoInteractions(postViewRepository, postViewService);
    }

    @Test
    @DisplayName("조회 사용자가 존재하지 않으면 게시글 조회에 실패한다")
    void getPostThrowsWhenViewerDoesNotExist() {
        Post post = post(10L, user(2L, "author"));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("조회 이력이 없으면 일반 조회수를 증가시키고 조회 이벤트를 전달한다")
    void getPostCreatesViewWhenHistoryDoesNotExist() {
        UserInfo viewer = user(1L, "viewer");
        Post post = post(10L, user(2L, "author"));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(viewer));
        when(postViewRepository
                .findByPost_PostNumAndUserInfo_ProfileId(10L, 1L))
                .thenReturn(Optional.empty());

        PostResponse response = postService.getPost(SIGN_USER, 10L);

        assertThat(response.viewCount()).isEqualTo(1);
        verify(postViewRepository).save(any(PostView.class));
        verify(postViewService).recordView(10L, WRITE_AT);
    }

    @Test
    @DisplayName("24시간 이내 조회 이력이 있으면 조회수를 다시 증가시키지 않는다")
    void getPostDoesNotCountRecentExistingView() {
        UserInfo viewer = user(1L, "viewer");
        Post post = post(10L, user(2L, "author"));
        PostView postView = mock(PostView.class);
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(viewer));
        when(postViewRepository
                .findByPost_PostNumAndUserInfo_ProfileId(10L, 1L))
                .thenReturn(Optional.of(postView));
        when(postView.view()).thenReturn(false);

        PostResponse response = postService.getPost(SIGN_USER, 10L);

        assertThat(response.viewCount()).isZero();
        verify(postViewRepository, never()).save(any(PostView.class));
        verifyNoInteractions(postViewService);
    }

    @Test
    @DisplayName("24시간이 지난 조회 이력은 일반 조회수와 인기글 조회 이벤트를 갱신한다")
    void getPostCountsExpiredExistingView() {
        UserInfo viewer = user(1L, "viewer");
        Post post = post(10L, user(2L, "author"));
        PostView postView = mock(PostView.class);
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(viewer));
        when(postViewRepository
                .findByPost_PostNumAndUserInfo_ProfileId(10L, 1L))
                .thenReturn(Optional.of(postView));
        when(postView.view()).thenReturn(true);

        PostResponse response = postService.getPost(SIGN_USER, 10L);

        assertThat(response.viewCount()).isEqualTo(1);
        verify(postViewService).recordView(10L, WRITE_AT);
    }

    @Test
    @DisplayName("관리자는 존재하는 게시글 원문을 조회한다")
    void adminGetPostReturnsExistingPost() {
        Post post = post(10L, user(2L, "author"));
        blind(post);
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        PostResponse response = postService.adminGetPost(10L);

        assertThat(response.title()).isEqualTo("title-10");
    }

    @Test
    @DisplayName("관리자 조회에서도 게시글이 없으면 예외를 던진다")
    void adminGetPostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.adminGetPost(10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @Test
    @DisplayName("게시글을 정상적으로 등록한다")
    void addPostCreatesPost() {
        UserInfo author = user(1L, "author");
        MultipartFile image = mock(MultipartFile.class);
        PostRequest request = new PostRequest("new-title", "new-content", image);
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(imageConverter.updatePostImage(image))
                .thenReturn("posts/new-image.png");
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedPost, "postNum", 10L);
            return savedPost;
        });

        PostResponse response = postService.addPost(SIGN_USER, request);

        assertThat(response.postNum()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("new-title");
        assertThat(response.image()).endsWith("posts/new-image.png");
    }

    @Test
    @DisplayName("게시글 작성 사용자가 없으면 등록에 실패한다")
    void addPostThrowsWhenUserDoesNotExist() {
        PostRequest request = new PostRequest("title", "content", null);
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.addPost(SIGN_USER, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("작성자는 기존 내용을 이력으로 남기고 게시글을 수정한다")
    void updatePostUpdatesPostForAuthor() {
        Post post = post(10L, user(1L, "author"));
        MultipartFile image = mock(MultipartFile.class);
        PostRequest request = new PostRequest(
                "updated-title",
                "updated-content",
                image
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(imageConverter.updatePostImage(image))
                .thenReturn("posts/updated.png");
        ArgumentCaptor<PostEditRecord> editCaptor =
                ArgumentCaptor.forClass(PostEditRecord.class);

        PostResponse response = postService.updatePost(
                SIGN_USER,
                10L,
                request
        );

        verify(postEditRepository).save(editCaptor.capture());
        assertThat(editCaptor.getValue().getTitle()).isEqualTo("title-10");
        assertThat(response.title()).isEqualTo("updated-title");
        assertThat(post.getContent()).isEqualTo("updated-content");
        assertThat(post.getImage()).isEqualTo("posts/updated.png");
    }

    @Test
    @DisplayName("관리자도 다른 사용자의 게시글은 수정할 수 없다")
    void updatePostThrowsWhenAdminIsNotAuthor() {
        Post post = post(10L, user(1L, "author"));
        PostRequest request = new PostRequest("admin-title", "content", null);
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(
                () -> postService.updatePost(ADMIN_USER, 10L, request)
        ).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");
        verifyNoInteractions(imageConverter, postEditRepository);
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 게시글을 수정할 수 없다")
    void updatePostThrowsWhenUserHasNoAuthority() {
        Post post = post(10L, user(2L, "author"));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        PostRequest request = new PostRequest("title", "content", null);

        assertThatThrownBy(
                () -> postService.updatePost(SIGN_USER, 10L, request)
        ).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 수정할 수 없다")
    void updatePostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());
        PostRequest request = new PostRequest("title", "content", null);

        assertThatThrownBy(
                () -> postService.updatePost(SIGN_USER, 10L, request)
        ).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

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

        PostLikeResponse response = postService.likePost(SIGN_USER, 10L);

        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(userLikeRepository).save(any(UserLikePost.class));
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

        PostLikeResponse response = postService.likePost(SIGN_USER, 10L);

        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(userLikeRepository).delete(userLikePost);
    }

    @Test
    @DisplayName("좋아요 사용자가 없으면 처리에 실패한다")
    void likePostThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.likePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("좋아요 대상 게시글이 없으면 처리에 실패한다")
    void likePostThrowsWhenPostDoesNotExist() {
        givenUser(user(1L, "viewer"));
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.likePost(SIGN_USER, 10L))
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

        boolean result = postService.isLikePost(SIGN_USER, 10L);

        assertThat(result).isEqualTo(liked);
    }

    @Test
    @DisplayName("좋아요 여부 조회 사용자가 없으면 예외를 던진다")
    void isLikePostThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.isLikePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("좋아요 여부 조회 게시글이 없으면 예외를 던진다")
    void isLikePostThrowsWhenPostDoesNotExist() {
        givenUser(user(1L, "viewer"));
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.isLikePost(SIGN_USER, 10L))
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

        PostReportResponse response = postService.reportPost(
                SIGN_USER,
                10L
        );

        assertThat(response.reportCount()).isEqualTo(1);
        verify(postReportRepository).save(any(PostReport.class));
    }

    @Test
    @DisplayName("본인이 작성한 게시글은 신고할 수 없다")
    void reportPostThrowsWhenReportingOwnPost() {
        UserInfo userInfo = user(1L, "author");
        Post post = post(10L, userInfo);
        givenPost(post);
        givenUser(userInfo);

        assertThatThrownBy(() -> postService.reportPost(SIGN_USER, 10L))
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

        assertThatThrownBy(() -> postService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(DuplicateException.class)
                .hasMessage("이미 신고한 게시글입니다.");
    }

    @Test
    @DisplayName("신고 대상 게시글이 없으면 예외를 던진다")
    void reportPostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    @Test
    @DisplayName("신고 사용자가 없으면 예외를 던진다")
    void reportPostThrowsWhenUserDoesNotExist() {
        givenPost(post(10L, user(2L, "author")));
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.reportPost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
    }

    @Test
    @DisplayName("인기글 통계에서 결정한 게시글 순서를 유지한다")
    void getTop10PopularPostsPreservesPopularityOrder() {
        Post first = post(1L, user(1L, "first-author"));
        Post second = post(2L, user(2L, "second-author"));
        Post third = post(3L, user(3L, "third-author"));
        when(postViewService.getTop10PopularPostNums())
                .thenReturn(List.of(3L, 2L, 1L));
        when(postRepository.findPostByPostNumIn(List.of(3L, 2L, 1L)))
                .thenReturn(List.of(first, second, third));

        PostSliceResponse response = postService.getTop10PopularPosts();

        assertThat(response.postTitleResponses())
                .extracting(PostTitleResponse::postNum)
                .containsExactly(3L, 2L, 1L);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("인기글 번호에 해당하는 게시글이 없으면 결과에서 제외한다")
    void getTop10PopularPostsSkipsMissingPosts() {
        Post first = post(1L, user(1L, "first-author"));
        Post third = post(3L, user(3L, "third-author"));
        when(postViewService.getTop10PopularPostNums())
                .thenReturn(List.of(3L, 2L, 1L));
        when(postRepository.findPostByPostNumIn(List.of(3L, 2L, 1L)))
                .thenReturn(List.of(first, third));

        PostSliceResponse response = postService.getTop10PopularPosts();

        assertThat(response.postTitleResponses())
                .extracting(PostTitleResponse::postNum)
                .containsExactly(3L, 1L);
    }

    @Test
    @DisplayName("작성자는 게시글을 삭제할 수 있다")
    void deletePostDeletesPostForAuthor() {
        Post post = post(10L, user(1L, "author"));
        givenPost(post);

        postService.deletePost(SIGN_USER, 10L);

        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("관리자는 다른 사용자의 게시글을 삭제할 수 있다")
    void deletePostAllowsAdmin() {
        Post post = post(10L, user(1L, "author"));
        givenPost(post);

        postService.deletePost(ADMIN_USER, 10L);

        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 게시글을 삭제할 수 없다")
    void deletePostThrowsWhenUserHasNoAuthority() {
        Post post = post(10L, user(2L, "author"));
        givenPost(post);

        assertThatThrownBy(() -> postService.deletePost(SIGN_USER, 10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");
        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 삭제할 수 없다")
    void deletePostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    private void givenPost(Post post) {
        when(postRepository.findByPostNum(post.getPostNum()))
                .thenReturn(Optional.of(post));
    }

    private void givenUser(UserInfo userInfo) {
        when(userInfoRepository.findByProfileId(userInfo.getProfileId()))
                .thenReturn(Optional.of(userInfo));
    }

    private UserInfo user(long profileId, String nickname) {
        UserInfo userInfo = new UserInfo(
                new SignInfo(nickname + "@example.com", "password"),
                nickname,
                null
        );
        userInfo.setProfileId(profileId);
        return userInfo;
    }

    private Post post(long postNum, UserInfo author) {
        Post post = new Post(
                author,
                "title-" + postNum,
                "content-" + postNum,
                null
        );
        ReflectionTestUtils.setField(post, "postNum", postNum);
        ReflectionTestUtils.setField(post, "writeAt", WRITE_AT);
        return post;
    }

    private PostEditRecord editRecord(long editId, Post post) {
        PostEditRecord editRecord = new PostEditRecord(
                post,
                0,
                "old-title",
                "old-content",
                null,
                WRITE_AT
        );
        ReflectionTestUtils.setField(editRecord, "editId", editId);
        return editRecord;
    }

    private void blind(Post post) {
        for (int reportCount = 0; reportCount < 6; reportCount++) {
            post.report();
        }
    }
}
