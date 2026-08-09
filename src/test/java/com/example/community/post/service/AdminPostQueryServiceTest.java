package com.example.community.post.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.dto.response.*;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
import com.example.community.post.repository.PostEditRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.community.post.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPostQueryServiceTest {
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

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    @InjectMocks
    private AdminPostQueryService adminPostQueryService;




    @Test
    @DisplayName("관리자 게시글 목록은 블라인드된 제목도 원문으로 반환한다")
    void adminGetPostsByPageReturnsOriginalTitle() {
        Post post = post(10L, user(1L, "author"));
        blind(post);
        Page<Post> page = new PageImpl<>(List.of(post));
        when(postRepository.findPostByPage(any(Pageable.class)))
                .thenReturn(page);

        AdminPostPageResponse response = adminPostQueryService.getPostsByPage(
                0,
                20,
                "latest"
        );

        assertThat(response.postTitleResponses())
                .singleElement()
                .extracting(AdminPostTitleResponse::title)
                .isEqualTo("title-10");
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

        PostEditPageResponse response = adminPostQueryService.getPostEditsByPage(
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

        PostEditResponse response = adminPostQueryService.getPostEdit(5L);

        assertThat(response.title()).isEqualTo("old-title");
        assertThat(response.content()).isEqualTo("old-content");
    }

    @Test
    @DisplayName("수정 이력이 없으면 예외를 던진다")
    void getPostEditThrowsWhenEditDoesNotExist() {
        when(postEditRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPostQueryService.getPostEdit(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 수정 이력");
    }







    @Test
    @DisplayName("관리자는 존재하는 게시글 원문을 조회한다")
    void adminGetPostReturnsExistingPost() {
        Post post = post(10L, user(2L, "author"));
        blind(post);
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));

        AdminPostResponse response = adminPostQueryService.getPost(10L);

        assertThat(response.title()).isEqualTo("title-10");
    }

    @Test
    @DisplayName("관리자 조회에서도 게시글이 없으면 예외를 던진다")
    void adminGetPostThrowsWhenPostDoesNotExist() {
        when(postRepository.findByPostNum(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminPostQueryService.getPost(10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");
    }



































    private void givenPost(Post post) {
        when(postRepository.findByPostNum(post.getPostNum()))
                .thenReturn(Optional.of(post));
    }

}
