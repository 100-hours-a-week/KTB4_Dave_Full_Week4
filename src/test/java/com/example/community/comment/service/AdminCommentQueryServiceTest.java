package com.example.community.comment.service;

import com.example.community.comment.dto.response.CommentEditPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
import com.example.community.comment.repository.CommentEditRepository;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.UserInfo;
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

import java.time.Instant;
import java.util.List;

import static com.example.community.comment.fixture.CommentTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCommentQueryServiceTest {
    private static final SignUserInfo AUTHOR =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo OTHER_USER =
            new SignUserInfo(2L, 2L, UserRole.USER);
    private static final SignUserInfo ADMIN =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentEditRepository commentEditRepository;

    @InjectMocks
    private AdminCommentQueryService adminCommentQueryService;











    @Test
    @DisplayName("관리자 목록은 감사 목적으로 삭제된 댓글 원문을 반환한다")
    void adminGetPostCommentPageReturnsDeletedCommentOriginalContent() {
        UserInfo author = user(1L, "author");
        Comment deleted = comment(
                30L,
                post(10L, author),
                author,
                "deleted-content"
        );
        deleted.delete();
        when(commentRepository.findByPost_postNum(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deleted)));

        CommentResponse response = adminCommentQueryService
                .getPostCommentPage(10L, 0, 10)
                .commentResponses()
                .getFirst();

        assertThat(response.content()).isEqualTo("deleted-content");
        assertThat(response.deleted()).isTrue();
    }

    @Test
    @DisplayName("관리자 대댓글 목록도 등록순 정렬을 적용한다")
    void adminGetChildCommentPageUsesRegistrationOrder() {
        when(commentRepository.findByParentNum(eq(20L), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        adminCommentQueryService.getChildCommentPage(20L, 0, 10);

        verify(commentRepository).findByParentNum(
                eq(20L),
                pageableCaptor.capture()
        );
        assertRegistrationOrder(pageableCaptor.getValue(), 0, 10);
    }

    @Test
    @DisplayName("댓글 수정 이력 Repository 결과를 응답으로 반환한다")
    void getCommentEditsByPageReturnsRepositoryPage() {
        Comment comment = comment(
                30L,
                post(10L, user(1L, "author")),
                user(1L, "author"),
                "current"
        );
        CommentEditRecord edit = new CommentEditRecord(
                5L,
                comment,
                0,
                "old-content",
                Instant.parse("2026-08-05T00:00:00Z")
        );
        when(commentEditRepository
                .findByComment_CommentNumOrderByEditIdDesc(
                        eq(30L),
                        any(Pageable.class)
                )).thenReturn(new PageImpl<>(List.of(edit)));

        CommentEditPageResponse response =
                adminCommentQueryService.getCommentEditsByPage(30L, 0, 10);

        assertThat(response.commentResponses())
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.editId()).isEqualTo(5L);
                    assertThat(found.content()).isEqualTo("old-content");
                });
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
