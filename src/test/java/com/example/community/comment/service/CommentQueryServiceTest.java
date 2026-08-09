package com.example.community.comment.service;

import com.example.community.comment.dto.response.CommentPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.example.community.comment.fixture.CommentTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceTest {
    private static final SignUserInfo AUTHOR =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo OTHER_USER =
            new SignUserInfo(2L, 2L, UserRole.USER);
    private static final SignUserInfo ADMIN =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentPageReader commentPageReader;

    @InjectMocks
    private CommentQueryService commentQueryService;








    @Test
    @DisplayName("최상위 댓글 목록 조회를 전용 읽기 서비스에 위임한다")
    void getPostCommentPageDelegatesToReadService() {
        CommentPageResponse expected = new CommentPageResponse(
                List.of(),
                2,
                20,
                0,
                0,
                0
        );
        when(commentPageReader.read(10L, 2, 20))
                .thenReturn(expected);

        CommentPageResponse result =
                commentQueryService.getPostCommentPage(10L, 2, 20);

        assertThat(result).isEqualTo(expected);
        verify(commentPageReader).read(10L, 2, 20);
    }

    @Test
    @DisplayName("대댓글 목록은 등록순 정렬과 요청 페이지를 적용한다")
    void getChildCommentPageUsesRegistrationOrder() {
        when(commentRepository.findByParentNum(eq(20L), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        commentQueryService.getChildCommentPage(20L, 1, 5);

        verify(commentRepository).findByParentNum(
                eq(20L),
                pageableCaptor.capture()
        );
        assertRegistrationOrder(pageableCaptor.getValue(), 1, 5);
    }

    @Test
    @DisplayName("일반 목록은 삭제된 댓글 내용을 마스킹해 반환한다")
    void getPostCommentPageMasksDeletedComment() {
        Comment deleted = comment(
                30L,
                post(10L, user(1L, "author")),
                user(1L, "author"),
                "deleted-content"
        );
        deleted.delete();
        when(commentPageReader.read(10L, 0, 10))
                .thenReturn(CommentPageResponse.from(
                        new PageImpl<>(List.of(deleted))
                ));

        CommentResponse response = commentQueryService
                .getPostCommentPage(10L, 0, 10)
                .commentResponses()
                .getFirst();

        assertThat(response.content()).isEqualTo("삭제된 댓글입니다.");
        assertThat(response.deleted()).isTrue();
    }













    private void assertRegistrationOrder(
            Pageable pageable,
            int page,
            int size
    ) {
        assertThat(pageable.getPageNumber()).isEqualTo(page);
        assertThat(pageable.getPageSize()).isEqualTo(size);
        assertThat(pageable.getSort().stream().toList())
                .singleElement()
                .satisfies(order -> {
                    assertThat(order.getProperty()).isEqualTo("commentNum");
                    assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
                });
    }

    private void assignCommentNumWhenSaved(long commentNum) {
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> {
                    Comment saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "commentNum", commentNum);
                    return saved;
                });
    }

}
