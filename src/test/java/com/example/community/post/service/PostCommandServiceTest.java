package com.example.community.post.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.ImageUploadException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostRequest;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.post.dto.response.PostResponse;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.event.PostChangedEvent;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.temporaryPost.repository.TemporaryPostRepository;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageConverter;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;

import static com.example.community.post.fixture.PostTestFixture.post;
import static com.example.community.post.fixture.PostTestFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCommandServiceTest {
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
    private UserInfoRepository userInfoRepository;

    @Mock
    private TemporaryPostRepository temporaryPostRepository;

    @Mock
    private ImageConverter imageConverter;

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PostImageResolver postImageResolver;

    private PostCommandService postCommandService;

    @BeforeEach
    void setUpCommandService() {
        postImageResolver = new PostImageResolver(
                temporaryPostRepository,
                imageConverter
        );
        postCommandService = new PostCommandService(
                postRepository,
                postEditRepository,
                userInfoRepository,
                postImageResolver,
                imageUrlBuilder,
                eventPublisher
        );
    }

















    @Test
    @DisplayName("게시글을 정상적으로 등록한다")
    void addPostCreatesPost() {
        UserInfo author = user(1L, "author");
        MultipartFile image = mock(MultipartFile.class);
        PostRequest request = new PostRequest(
                "new-title",
                "new-content",
                null,
                null,
                image
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(imageConverter.updatePostImage(image))
                .thenReturn("posts/new-image.png");
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedPost, "postNum", 10L);
            return savedPost;
        });

        PostResponse response = postCommandService.addPost(SIGN_USER, request);

        assertThat(response.postNum()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("new-title");
        assertThat(response.image()).endsWith("posts/new-image.png");
    }

    @Test
    @DisplayName("게시글 작성 시 본인 임시글의 objectKey를 재사용한다")
    void addPostReusesOwnedTemporaryPostObjectKey() {
        UserInfo author = user(1L, "author");
        PostRequest request = new PostRequest(
                "new-title",
                "new-content",
                15L,
                "posts/temporary.png",
                null
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        TemporaryPost temporaryPost = new TemporaryPost(author);
        temporaryPost.update(
                "temporary-title",
                "temporary-content",
                "posts/temporary.png"
        );
        when(temporaryPostRepository
                .findByTemporaryIdAndUserInfo_ProfileId(15L, 1L))
                .thenReturn(Optional.of(temporaryPost));
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post savedPost = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedPost, "postNum", 10L);
            return savedPost;
        });
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        postCommandService.addPost(SIGN_USER, request);

        verify(postRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getImage())
                .isEqualTo("posts/temporary.png");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("임시저장글 없이 objectKey만 전달하면 게시글 작성을 거부한다")
    void addPostRejectsObjectKeyWithoutTemporaryPost() {
        UserInfo author = user(1L, "author");
        PostRequest request = new PostRequest(
                "new-title",
                "new-content",
                null,
                "posts/unverified.png",
                null
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));

        assertThatThrownBy(() -> postCommandService.addPost(SIGN_USER, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        verifyNoInteractions(temporaryPostRepository, imageConverter);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("게시글 작성 시 본인 임시글에 없는 objectKey는 거부한다")
    void addPostRejectsUnownedObjectKey() {
        UserInfo author = user(1L, "author");
        PostRequest request = new PostRequest(
                "new-title",
                "new-content",
                15L,
                "posts/other.png",
                null
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        TemporaryPost temporaryPost = new TemporaryPost(author);
        temporaryPost.update(
                "temporary-title",
                "temporary-content",
                "posts/current.png"
        );
        when(temporaryPostRepository
                .findByTemporaryIdAndUserInfo_ProfileId(15L, 1L))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> postCommandService.addPost(SIGN_USER, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        verify(postRepository, never()).save(any(Post.class));
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("게시글 작성 시 본인 소유가 아닌 임시글은 거부한다")
    void addPostRejectsUnownedTemporaryPost() {
        UserInfo author = user(1L, "author");
        PostRequest request = new PostRequest(
                "new-title",
                "new-content",
                15L,
                "posts/temporary.png",
                null
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(temporaryPostRepository
                .findByTemporaryIdAndUserInfo_ProfileId(15L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.addPost(SIGN_USER, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 임시저장글입니다.");

        verify(postRepository, never()).save(any(Post.class));
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("게시글 작성 사용자가 없으면 등록에 실패한다")
    void addPostThrowsWhenUserDoesNotExist() {
        PostRequest request = new PostRequest(
                "title",
                "content",
                null,
                null,
                null
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.addPost(SIGN_USER, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("작성자는 기존 내용을 이력으로 남기고 게시글을 수정한다")
    void updatePostUpdatesPostForAuthor() {
        Post post = post(10L, user(1L, "author"));
        ReflectionTestUtils.setField(post, "image", "posts/old.png");
        MultipartFile image = mock(MultipartFile.class);
        PostUpdateRequest request = new PostUpdateRequest(
                "updated-title",
                "updated-content",
                null,
                image
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(imageConverter.updatePostImage(image))
                .thenReturn("posts/updated.png");
        ArgumentCaptor<PostEditRecord> editCaptor =
                ArgumentCaptor.forClass(PostEditRecord.class);

        PostResponse response = postCommandService.updatePost(
                SIGN_USER,
                10L,
                request
        );

        verify(postEditRepository).save(editCaptor.capture());
        assertThat(editCaptor.getValue().getTitle()).isEqualTo("title-10");
        assertThat(editCaptor.getValue().getImage()).isEqualTo("posts/old.png");
        assertThat(response.title()).isEqualTo("updated-title");
        assertThat(post.getContent()).isEqualTo("updated-content");
        assertThat(post.getImage()).isEqualTo("posts/updated.png");
        verify(eventPublisher).publishEvent(new PostChangedEvent.Updated(10L));
    }

    @Test
    @DisplayName("현재 objectKey를 전달하면 기존 게시글 이미지를 유지한다")
    void updatePostKeepsExistingImage() {
        Post post = post(10L, user(1L, "author"));
        ReflectionTestUtils.setField(post, "image", "posts/old.png");
        PostUpdateRequest request = new PostUpdateRequest(
                "updated-title",
                "updated-content",
                "posts/old.png",
                null
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        postCommandService.updatePost(SIGN_USER, 10L, request);

        assertThat(post.getImage()).isEqualTo("posts/old.png");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("게시글 수정 시 현재 값과 다른 objectKey는 거부한다")
    void updatePostRejectsMismatchedObjectKey() {
        Post post = post(10L, user(1L, "author"));
        ReflectionTestUtils.setField(post, "image", "posts/old.png");
        PostUpdateRequest request = new PostUpdateRequest(
                "updated-title",
                "updated-content",
                "posts/other.png",
                null
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(
                () -> postCommandService.updatePost(SIGN_USER, 10L, request)
        ).isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        assertThat(post.getImage()).isEqualTo("posts/old.png");
        verifyNoInteractions(imageConverter, postEditRepository);
    }

    @Test
    @DisplayName("objectKey와 이미지가 없으면 게시글 이미지를 삭제한다")
    void updatePostDeletesExistingImage() {
        Post post = post(10L, user(1L, "author"));
        ReflectionTestUtils.setField(post, "image", "posts/old.png");
        PostUpdateRequest request = new PostUpdateRequest(
                "updated-title",
                "updated-content",
                null,
                null
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        ArgumentCaptor<PostEditRecord> editCaptor =
                ArgumentCaptor.forClass(PostEditRecord.class);

        postCommandService.updatePost(SIGN_USER, 10L, request);

        verify(postEditRepository).save(editCaptor.capture());
        assertThat(editCaptor.getValue().getImage()).isEqualTo("posts/old.png");
        assertThat(post.getImage()).isNull();
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("objectKey 없이 빈 이미지가 오면 게시글 이미지를 삭제한다")
    void updatePostDeletesExistingImageForEmptyFile() {
        Post post = post(10L, user(1L, "author"));
        ReflectionTestUtils.setField(post, "image", "posts/old.png");
        MultipartFile emptyImage = mock(MultipartFile.class);
        when(emptyImage.isEmpty()).thenReturn(true);
        PostUpdateRequest request = new PostUpdateRequest(
                "updated-title",
                "updated-content",
                null,
                emptyImage
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        postCommandService.updatePost(SIGN_USER, 10L, request);

        assertThat(post.getImage()).isNull();
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("게시글 이미지 업로드에 실패하면 기존 값과 수정 이력을 변경하지 않는다")
    void updatePostDoesNotChangePostWhenImageUploadFails() {
        Post post = post(10L, user(1L, "author"));
        ReflectionTestUtils.setField(post, "image", "posts/old.png");
        MultipartFile image = mock(MultipartFile.class);
        PostUpdateRequest request = new PostUpdateRequest(
                "updated-title",
                "updated-content",
                null,
                image
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(imageConverter.updatePostImage(image))
                .thenThrow(new ImageUploadException("upload failed", null));

        assertThatThrownBy(() -> postCommandService.updatePost(
                SIGN_USER,
                10L,
                request
        )).isInstanceOf(ImageUploadException.class);

        assertThat(post.getTitle()).isEqualTo("title-10");
        assertThat(post.getContent()).isEqualTo("content-10");
        assertThat(post.getImage()).isEqualTo("posts/old.png");
        verifyNoInteractions(postEditRepository);
    }

    @Test
    @DisplayName("관리자도 다른 사용자의 게시글은 수정할 수 없다")
    void updatePostThrowsWhenAdminIsNotAuthor() {
        Post post = post(10L, user(1L, "author"));
        PostUpdateRequest request = new PostUpdateRequest(
                "admin-title",
                "content",
                null,
                null
        );
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        assertThatThrownBy(
                () -> postCommandService.updatePost(ADMIN_USER, 10L, request)
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
        PostUpdateRequest request = new PostUpdateRequest(
                "title",
                "content",
                null,
                null
        );

        assertThatThrownBy(
                () -> postCommandService.updatePost(SIGN_USER, 10L, request)
        ).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 수정할 수 없다")
    void updatePostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());
        PostUpdateRequest request = new PostUpdateRequest(
                "title",
                "content",
                null,
                null
        );

        assertThatThrownBy(
                () -> postCommandService.updatePost(SIGN_USER, 10L, request)
        ).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }
















    @Test
    @DisplayName("작성자는 게시글을 삭제할 수 있다")
    void deletePostDeletesPostForAuthor() {
        Post post = post(10L, user(1L, "author"));
        givenPost(post);

        postCommandService.deletePost(SIGN_USER, 10L);

        assertThat(post.getDeletedAt()).isNotNull();
        verify(eventPublisher).publishEvent(new PostChangedEvent.Removed(10L));
    }

    @Test
    @DisplayName("관리자는 다른 사용자의 게시글을 삭제할 수 있다")
    void deletePostAllowsAdmin() {
        Post post = post(10L, user(1L, "author"));
        givenPost(post);

        postCommandService.deletePost(ADMIN_USER, 10L);

        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 게시글을 삭제할 수 없다")
    void deletePostThrowsWhenUserHasNoAuthority() {
        Post post = post(10L, user(2L, "author"));
        givenPost(post);

        assertThatThrownBy(() -> postCommandService.deletePost(SIGN_USER, 10L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");
        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 게시글은 삭제할 수 없다")
    void deletePostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postCommandService.deletePost(SIGN_USER, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }

    private void givenPost(Post post) {
        when(postRepository.findByPostNum(post.getPostNum()))
                .thenReturn(Optional.of(post));
    }

}
