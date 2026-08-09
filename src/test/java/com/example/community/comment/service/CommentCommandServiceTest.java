package com.example.community.comment.service;

import com.example.community.comment.dto.request.CommentEditRequest;
import com.example.community.comment.dto.request.CommentToCommentRequest;
import com.example.community.comment.dto.request.CommentToPostRequest;
import com.example.community.comment.dto.response.CommentAddResponse;
import com.example.community.comment.dto.response.CommentResponse;
import com.example.community.comment.entity.Comment;
import com.example.community.comment.entity.CommentEditRecord;
import com.example.community.comment.event.CommentChangedEvent;
import com.example.community.comment.repository.CommentEditRepository;
import com.example.community.comment.repository.CommentRepository;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.ForbiddenException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.resolver.SignUserInfo;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static com.example.community.comment.fixture.CommentTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentCommandService commentCommandService;

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

        CommentAddResponse response = commentCommandService.addCommentToPost(
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
        verify(eventPublisher).publishEvent(new CommentChangedEvent.Created(
                10L,
                null
        ));
    }

    @Test
    @DisplayName("댓글 작성 사용자가 없으면 예외를 던진다")
    void addCommentToPostThrowsWhenUserDoesNotExist() {
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.addCommentToPost(
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

        assertThatThrownBy(() -> commentCommandService.addCommentToPost(
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

        assertThatThrownBy(() -> commentCommandService.addCommentToComment(
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

        assertThatThrownBy(() -> commentCommandService.addCommentToComment(
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
        when(commentRepository
                .findByCommentNumAndPost_PostNumAndDeletedAtIsNull(20L, 10L))
                .thenReturn(Optional.of(parent));
        assignCommentNumWhenSaved(21L);

        CommentAddResponse response = commentCommandService.addCommentToComment(
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
        verify(eventPublisher).publishEvent(new CommentChangedEvent.Created(
                10L,
                20L
        ));
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
        when(commentRepository
                .findByCommentNumAndPost_PostNumAndDeletedAtIsNull(20L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.addCommentToComment(
                AUTHOR,
                10L,
                new CommentToCommentRequest("child", 20L)
        )).isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 댓글");

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("다른 게시글의 댓글에는 대댓글을 등록할 수 없다")
    void addCommentToCommentRejectsParentFromDifferentPost() {
        UserInfo author = user(1L, "author");
        Post requestedPost = post(10L, author);
        Post parentPost = post(11L, author);
        Comment parent = comment(20L, parentPost, author, "parent");
        when(userInfoRepository.findByProfileId(1L))
                .thenReturn(Optional.of(author));
        when(postRepository.findByPostNum(10L))
                .thenReturn(Optional.of(requestedPost));
        when(commentRepository
                .findByCommentNumAndPost_PostNumAndDeletedAtIsNull(20L, 10L))
                .thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentCommandService.addCommentToComment(
                AUTHOR,
                10L,
                new CommentToCommentRequest("child", 20L)
        )).isInstanceOf(BadRequestException.class)
                .hasMessage(
                        "부모 댓글과 같은 게시글에만 답글을 작성할 수 있습니다."
                );

        verify(commentRepository, never()).save(any(Comment.class));
        verify(postRepository, never()).save(any(Post.class));
        verifyNoInteractions(eventPublisher);
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

        CommentResponse response = commentCommandService.updateComment(
                AUTHOR,
                30L,
                new CommentEditRequest("updated-content")
        );

        verify(commentEditRepository).save(editCaptor.capture());
        assertThat(editCaptor.getValue().getContent()).isEqualTo("old-content");
        assertThat(response.content()).isEqualTo("updated-content");
        assertThat(response.edited()).isTrue();
        verify(commentRepository).save(comment);
        verify(eventPublisher).publishEvent(new CommentChangedEvent.Updated(30L));
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

        assertThatThrownBy(() -> commentCommandService.updateComment(
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

        assertThatThrownBy(() -> commentCommandService.updateComment(
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

        assertThatThrownBy(() -> commentCommandService.updateComment(
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

        commentCommandService.deleteComment(AUTHOR, 30L);

        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
        verify(eventPublisher).publishEvent(new CommentChangedEvent.Deleted(
                30L,
                null
        ));
    }

    @Test
    @DisplayName("대댓글 삭제는 해당 댓글과 부모 댓글 변경을 함께 발행한다")
    void deleteReplyPublishesParentChange() {
        UserInfo author = user(1L, "author");
        Post post = post(10L, author);
        Comment parent = comment(20L, post, author, "parent");
        Comment reply = new Comment(post, parent, author, "reply");
        ReflectionTestUtils.setField(reply, "commentNum", 30L);
        when(commentRepository.findByCommentNum(30L))
                .thenReturn(Optional.of(reply));

        commentCommandService.deleteComment(AUTHOR, 30L);

        assertThat(reply.isDeleted()).isTrue();
        verify(eventPublisher).publishEvent(new CommentChangedEvent.Deleted(
                30L,
                20L
        ));
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

        commentCommandService.deleteComment(ADMIN, 30L);

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

        assertThatThrownBy(() -> commentCommandService.deleteComment(
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

        assertThatThrownBy(() -> commentCommandService.deleteComment(AUTHOR, 30L))
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

}
