package com.example.community.temporaryPost.service;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.request.PostUpdateRequest;
import com.example.community.resolver.SignUserInfo;
import com.example.community.temporaryPost.dto.request.TemporaryPostRequest;
import com.example.community.temporaryPost.dto.response.TemporaryKeyResponse;
import com.example.community.temporaryPost.dto.response.TemporaryPostResponse;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Optional;

import static com.example.community.temporaryPost.fixture.TemporaryPostTestFixture.temporaryPost;
import static com.example.community.temporaryPost.fixture.TemporaryPostTestFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemporaryPostCommandServiceTest {

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

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private ImageConverter imageConverter;

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    private TemporaryPostAccess temporaryPostAccess;

    private TemporaryPostCommandService temporaryPostCommandService;

    @BeforeEach
    void setUpServices() {
        temporaryPostAccess = new TemporaryPostAccess(
                temporaryPostRepository
        );
        temporaryPostCommandService = new TemporaryPostCommandService(
                temporaryPostRepository,
                userInfoRepository,
                temporaryPostAccess,
                imageConverter,
                imageUrlBuilder
        );
    }

    @Test
    @DisplayName("첫 임시저장 시 내용과 이미지를 저장하고 키를 반환한다")
    void createTemporaryPostSavesContentAndReturnsKey() {
        UserInfo owner = user(1L, "owner");
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        TemporaryPostRequest request = new TemporaryPostRequest(
                "title",
                "content",
                image
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(owner));
        when(imageConverter.updatePostImage(image))
                .thenReturn("posts/temporary.png");
        when(temporaryPostRepository.save(any(TemporaryPost.class)))
                .thenAnswer(invocation -> {
                    TemporaryPost savedPost = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            savedPost,
                            "temporaryId",
                            TEMPORARY_ID
                    );
                    return savedPost;
                });
        ArgumentCaptor<TemporaryPost> postCaptor =
                ArgumentCaptor.forClass(TemporaryPost.class);

        TemporaryKeyResponse response =
                temporaryPostCommandService.createTemporaryPost(OWNER, request);

        verify(temporaryPostRepository).save(postCaptor.capture());
        assertThat(postCaptor.getValue().getUserInfo()).isSameAs(owner);
        assertThat(postCaptor.getValue().getTitle()).isEqualTo("title");
        assertThat(postCaptor.getValue().getContent()).isEqualTo("content");
        assertThat(postCaptor.getValue().getImage())
                .isEqualTo("posts/temporary.png");
        assertThat(response.temporaryKeyId()).isEqualTo(TEMPORARY_ID);
        assertThat(response.objectKey()).isEqualTo("posts/temporary.png");
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 첫 임시저장을 할 수 없다")
    void createTemporaryPostThrowsWhenUserDoesNotExist() {
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        TemporaryPostRequest request = new TemporaryPostRequest(
                "title",
                "content",
                image
        );
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> temporaryPostCommandService.createTemporaryPost(
                OWNER,
                request
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");
        verify(temporaryPostRepository, never())
                .save(any(TemporaryPost.class));
        verifyNoInteractions(imageConverter);
    }






    @Test
    @DisplayName("objectKey와 이미지가 없으면 임시저장글 이미지를 삭제한다")
    void updateTemporaryPostUpdatesWithoutImageConversion() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                "posts/old.png"
        );
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                null,
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        TemporaryPostResponse response = temporaryPostCommandService
                .updateTemporaryPost(OWNER, TEMPORARY_ID, request);

        assertThat(response.title()).isEqualTo("new-title");
        assertThat(response.content()).isEqualTo("new-content");
        assertThat(response.image()).isNull();
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("현재 objectKey를 전달하면 기존 임시저장글 이미지를 유지한다")
    void updateTemporaryPostKeepsExistingImage() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                "posts/old.png"
        );
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                "posts/old.png",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        TemporaryPostResponse response = temporaryPostCommandService
                .updateTemporaryPost(OWNER, TEMPORARY_ID, request);

        assertThat(response.image()).endsWith("posts/old.png");
        assertThat(response.objectKey()).isEqualTo("posts/old.png");
        assertThat(temporaryPost.getImage()).isEqualTo("posts/old.png");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("임시글 수정 시 현재 값과 다른 objectKey는 거부한다")
    void updateTemporaryPostRejectsMismatchedObjectKey() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                "posts/old.png"
        );
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                "posts/other.png",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostCommandService.updateTemporaryPost(
                OWNER,
                TEMPORARY_ID,
                request
        )).isInstanceOf(BadRequestException.class)
                .hasMessage("유효하지 않은 objectKey입니다.");

        assertThat(temporaryPost.getImage()).isEqualTo("posts/old.png");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("objectKey 없이 빈 이미지가 오면 임시저장글 이미지를 삭제한다")
    void updateTemporaryPostDeletesImageForEmptyFile() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                "posts/old.png"
        );
        MultipartFile emptyImage = org.mockito.Mockito.mock(MultipartFile.class);
        when(emptyImage.isEmpty()).thenReturn(true);
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                null,
                emptyImage
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        temporaryPostCommandService.updateTemporaryPost(OWNER, TEMPORARY_ID, request);

        assertThat(temporaryPost.getImage()).isNull();
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("이미지가 있으면 변환 결과로 임시저장글을 수정한다")
    void updateTemporaryPostUsesConvertedImage() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                null
        );
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                null,
                image
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));
        when(imageConverter.updatePostImage(image))
                .thenReturn("posts/converted.png");

        TemporaryPostResponse response = temporaryPostCommandService
                .updateTemporaryPost(OWNER, TEMPORARY_ID, request);

        verify(imageConverter).updatePostImage(image);
        assertThat(response.image()).endsWith("posts/converted.png");
        assertThat(response.objectKey()).isEqualTo("posts/converted.png");
        assertThat(temporaryPost.getImage()).isEqualTo("posts/converted.png");
    }

    @Test
    @DisplayName("권한이 없으면 임시저장글을 수정하지 않는다")
    void updateTemporaryPostThrowsWhenUserIsNotOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                null
        );
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                null,
                image
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostCommandService.updateTemporaryPost(
                OTHER_USER,
                TEMPORARY_ID,
                request
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근권한 부족");
        assertThat(temporaryPost.getTitle()).isEqualTo("old-title");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("관리자도 다른 사용자의 임시저장글을 수정할 수 없다")
    void updateTemporaryPostThrowsWhenAdminIsNotOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "old-title",
                "old-content",
                null
        );
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        PostUpdateRequest request = new PostUpdateRequest(
                "new-title",
                "new-content",
                null,
                image
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostCommandService.updateTemporaryPost(
                ADMIN,
                TEMPORARY_ID,
                request
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근권한 부족");
        assertThat(temporaryPost.getTitle()).isEqualTo("old-title");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글은 수정하지 않는다")
    void updateTemporaryPostThrowsWhenTemporaryPostDoesNotExist() {
        PostUpdateRequest request = new PostUpdateRequest(
                "title",
                "content",
                null,
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> temporaryPostCommandService.updateTemporaryPost(
                OWNER,
                TEMPORARY_ID,
                request
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 임시저장글");
        verifyNoInteractions(imageConverter);
    }

    @Test
    @DisplayName("소유자는 임시저장글을 삭제할 수 있다")
    void deleteTemporaryPostDeletesPostForOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "title",
                "content",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        temporaryPostCommandService.deleteTemporaryPost(OWNER, TEMPORARY_ID);

        verify(temporaryPostRepository).delete(temporaryPost);
    }

    @Test
    @DisplayName("일반 사용자는 다른 사용자의 임시저장글을 삭제할 수 없다")
    void deleteTemporaryPostThrowsWhenUserIsNotOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "title",
                "content",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostCommandService.deleteTemporaryPost(
                OTHER_USER,
                TEMPORARY_ID
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근권한 부족");
        verify(temporaryPostRepository, never())
                .delete(any(TemporaryPost.class));
    }

    @Test
    @DisplayName("관리자도 다른 사용자의 임시저장글을 삭제할 수 없다")
    void deleteTemporaryPostThrowsWhenAdminIsNotOwner() {
        TemporaryPost temporaryPost = temporaryPost(
                TEMPORARY_ID,
                user(1L, "owner"),
                "title",
                "content",
                null
        );
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.of(temporaryPost));

        assertThatThrownBy(() -> temporaryPostCommandService.deleteTemporaryPost(
                ADMIN,
                TEMPORARY_ID
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근권한 부족");
        verify(temporaryPostRepository, never())
                .delete(any(TemporaryPost.class));
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글은 삭제하지 않는다")
    void deleteTemporaryPostThrowsWhenTemporaryPostDoesNotExist() {
        when(temporaryPostRepository.findByTemporaryId(TEMPORARY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> temporaryPostCommandService.deleteTemporaryPost(
                OWNER,
                TEMPORARY_ID
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 임시저장글");
        verify(temporaryPostRepository, never())
                .delete(any(TemporaryPost.class));
    }

}
