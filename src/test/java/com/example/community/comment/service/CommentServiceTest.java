package com.example.community.comment.service;

import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentEditPageResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
import com.example.community.comment.repository.CommentEditRepository;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    private static final SignUserInfo AUTHOR =
            new SignUserInfo(1L, 1L, UserRole.USER);
    private static final SignUserInfo OTHER_USER =
            new SignUserInfo(2L, 2L, UserRole.USER);
    private static final SignUserInfo ADMIN =
            new SignUserInfo(99L, 99L, UserRole.ADMIN);

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private CommentEditRepository commentEditRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("게시글에 댓글을 등록하고 증가한 댓글 수와 등록한 댓글을 반환한다")
    void addCommentToPostReturnsSavedCommentAndCount() {
        UserInfo author = user(1L, "author");
        Post post = post(10L, author);
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        assignCommentNumWhenSaved(100L);

        CommentAddResponse response = commentService.addCommentToPost(
                AUTHOR,
                10L,
                new CommentToPostRequest("comment")
        );
        ArgumentCaptor<Comment> commentCaptor =
                ArgumentCaptor.forClass(Comment.class);

        assertThat(response.numberOfComments()).isEqualTo(1);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(response.comment())
                .isEqualTo(CommentResponse.from(commentCaptor.getValue()));
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("댓글 작성 사용자가 없으면 예외를 던진다")
    void addCommentToPostThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addCommentToPost(
                AUTHOR,
                10L,
                new CommentToPostRequest("comment")
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");

        verifyNoInteractions(postRepository, commentRepository);
    }

    @Test
    @DisplayName("댓글을 작성할 게시글이 없으면 예외를 던진다")
    void addCommentToPostThrowsWhenPostDoesNotExist() {
        UserInfo author = user(1L, "author");
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addCommentToPost(
                AUTHOR,
                10L,
                new CommentToPostRequest("comment")
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");

        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("대댓글 작성 사용자가 없으면 예외를 던진다")
    void addCommentToCommentThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addCommentToComment(
                AUTHOR,
                10L,
                new CommentToCommentRequest("child", 20L)
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");

        verifyNoInteractions(postRepository, commentRepository);
    }

    @Test
    @DisplayName("대댓글을 작성할 게시글이 없으면 예외를 던진다")
    void addCommentToCommentThrowsWhenPostDoesNotExist() {
        UserInfo author = user(1L, "author");
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addCommentToComment(
                AUTHOR,
                10L,
                new CommentToCommentRequest("child", 20L)
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 게시글");

        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("대댓글을 등록하고 증가한 댓글 수와 등록한 대댓글을 반환한다")
    void addCommentToCommentReturnsSavedChildComment() {
        UserInfo author = user(1L, "author");
        Post post = post(10L, author);
        Comment parent = comment(20L, post, author, "parent");
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(commentRepository.findByCommentNum(20L))
                .thenReturn(Optional.of(parent));
        assignCommentNumWhenSaved(21L);

        CommentAddResponse response = commentService.addCommentToComment(
                AUTHOR,
                10L,
                new CommentToCommentRequest("child", 20L)
        );
        ArgumentCaptor<Comment> commentCaptor =
                ArgumentCaptor.forClass(Comment.class);

        assertThat(response.numberOfComments()).isEqualTo(2);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(response.comment())
                .isEqualTo(CommentResponse.from(commentCaptor.getValue()));
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("부모 댓글이 없으면 대댓글 등록에 실패한다")
    void addCommentToCommentThrowsWhenParentDoesNotExist() {
        UserInfo author = user(1L, "author");
        Post post = post(10L, author);
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(post));
        when(commentRepository.findByCommentNum(20L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addCommentToComment(
                AUTHOR,
                10L,
                new CommentToCommentRequest("child", 20L)
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 댓글");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("최상위 댓글 목록은 등록순 정렬과 요청 페이지를 적용한다")
    void getPostCommentPageUsesRegistrationOrder() {
        when(commentRepository.findByPost_postNum(eq(10L), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        commentService.getPostCommentPage(10L, 2, 20);

        verify(commentRepository).findByPost_postNum(
                eq(10L),
                pageableCaptor.capture()
        );
        assertRegistrationOrder(pageableCaptor.getValue(), 2, 20);
    }

    @Test
    @DisplayName("대댓글 목록은 등록순 정렬과 요청 페이지를 적용한다")
    void getChildCommentPageUsesRegistrationOrder() {
        when(commentRepository.findByParentNum(eq(20L), any(Pageable.class)))
                .thenReturn(Page.empty());
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        commentService.getChildCommentPage(20L, 1, 5);

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
        when(commentRepository.findByPost_postNum(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deleted)));

        CommentResponse response = commentService
                .getPostCommentPage(10L, 0, 10)
                .commentResponses()
                .getFirst();

        assertThat(response.content()).isEqualTo("삭제된 댓글입니다.");
        assertThat(response.deleted()).isTrue();
    }

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

        CommentResponse response = commentService
                .adminGetPostCommentPage(10L, 0, 10)
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

        commentService.adminGetChildCommentPage(20L, 0, 10);

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
                commentService.getCommentEditsByPage(30L, 0, 10);

        assertThat(response.commentResponses())
                .singleElement()
                .satisfies(found -> {
                    assertThat(found.editId()).isEqualTo(5L);
                    assertThat(found.content()).isEqualTo("old-content");
                });
    }

    @Test
    @DisplayName("작성자는 기존 내용을 이력으로 남기고 댓글을 수정한다")
    void updateCommentUpdatesCommentForAuthor() {
        UserInfo author = user(1L, "author");
        Comment comment = comment(
                30L,
                post(10L, author),
                author,
                "old-content"
        );
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(comment));
        ArgumentCaptor<CommentEditRecord> editCaptor =
                ArgumentCaptor.forClass(CommentEditRecord.class);

        CommentResponse response = commentService.updateComment(
                AUTHOR,
                30L,
                new CommentEditRequest("updated-content")
        );

        verify(commentEditRepository).save(editCaptor.capture());
        assertThat(editCaptor.getValue().getContent()).isEqualTo("old-content");
        assertThat(response.content()).isEqualTo("updated-content");
        assertThat(response.edited()).isTrue();
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("관리자도 다른 사용자의 댓글은 수정할 수 없다")
    void updateCommentThrowsWhenAdminIsNotAuthor() {
        Comment comment = comment(
                30L,
                post(10L, user(1L, "author")),
                user(1L, "author"),
                "content"
        );
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                ADMIN,
                30L,
                new CommentEditRequest("admin-edit")
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");

        assertThat(comment.getContent()).isEqualTo("content");
        verifyNoInteractions(commentEditRepository);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 댓글을 수정할 수 없다")
    void updateCommentThrowsWhenUserIsNotAuthor() {
        Comment comment = comment(
                30L,
                post(10L, user(1L, "author")),
                user(1L, "author"),
                "content"
        );
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                OTHER_USER,
                30L,
                new CommentEditRequest("other-edit")
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");

        verifyNoInteractions(commentEditRepository);
    }

    @Test
    @DisplayName("존재하지 않는 댓글은 수정할 수 없다")
    void updateCommentThrowsWhenCommentDoesNotExist() {
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(
                AUTHOR,
                30L,
                new CommentEditRequest("updated")
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 댓글");

        verifyNoInteractions(commentEditRepository);
    }

    @Test
    @DisplayName("작성자는 댓글을 삭제할 수 있다")
    void deleteCommentDeletesCommentForAuthor() {
        UserInfo author = user(1L, "author");
        Comment comment = comment(
                30L,
                post(10L, author),
                author,
                "content"
        );
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(comment));

        commentService.deleteComment(AUTHOR, 30L);

        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("관리자는 다른 사용자의 댓글을 삭제할 수 있다")
    void deleteCommentAllowsAdmin() {
        UserInfo author = user(1L, "author");
        Comment comment = comment(
                30L,
                post(10L, author),
                author,
                "content"
        );
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(comment));

        commentService.deleteComment(ADMIN, 30L);

        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 댓글을 삭제할 수 없다")
    void deleteCommentThrowsWhenUserHasNoAuthority() {
        Comment comment = comment(
                30L,
                post(10L, user(1L, "author")),
                user(1L, "author"),
                "content"
        );
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(
                OTHER_USER,
                30L
        )).isInstanceOf(ForbiddenException.class)
                .hasMessage("접근 권한 부족");

        assertThat(comment.isDeleted()).isFalse();
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("존재하지 않는 댓글은 삭제할 수 없다")
    void deleteCommentThrowsWhenCommentDoesNotExist() {
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(AUTHOR, 30L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 댓글");
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
        Post post = new Post(author, "title", "content", null);
        ReflectionTestUtils.setField(post, "postNum", postNum);
        return post;
    }

    private Comment comment(
            long commentNum,
            Post post,
            UserInfo author,
            String content
    ) {
        Comment comment = new Comment(post, author, content);
        ReflectionTestUtils.setField(comment, "commentNum", commentNum);
        return comment;
    }
}
