package com.example.community.comment.entity;

import com.example.community.handler.exception.BadRequestException;
import com.example.community.post.entity.Post;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    private Post post;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        userInfo = new UserInfo(
                new SignInfo("author@example.com", "encoded-password"),
                "author",
                null
        );
        post = new Post(userInfo, "title", "content", null);
    }

    @Test
    @DisplayName("최상위 댓글 생성 시 필수 인자가 없으면 거부한다")
    void rootCommentRejectsMissingRequiredArguments() {
        assertThatThrownBy(() -> new Comment(null, userInfo, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");
        assertThatThrownBy(() -> new Comment(post, null, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");
        assertThatThrownBy(() -> new Comment(post, userInfo, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");

        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("답글 생성 시 필수 인자가 없으면 거부한다")
    void childCommentRejectsMissingRequiredArguments() {
        Comment parent = new Comment(post, userInfo, "parent");

        assertThatThrownBy(() ->
                new Comment(null, parent, userInfo, "child")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");
        assertThatThrownBy(() ->
                new Comment(post, parent, null, "child")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");
        assertThatThrownBy(() ->
                new Comment(post, null, userInfo, "child")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");
        assertThatThrownBy(() ->
                new Comment(post, parent, userInfo, " ")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필수 인자가 비어있습니다.");

        assertThat(parent.getChildCount()).isZero();
        assertThat(post.getCommentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("답글 생성 시 부모 깊이와 댓글 수를 반영한다")
    void childCommentReflectsDepthAndCounts() {
        Comment parent = new Comment(post, userInfo, "parent");

        Comment child = new Comment(post, parent, userInfo, "child");

        assertThat(child.getDepth()).isEqualTo(1);
        assertThat(child.getComment()).isSameAs(parent);
        assertThat(parent.getChildCount()).isEqualTo(1);
        assertThat(post.getCommentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("깊이 3 댓글에는 답글을 생성할 수 없다")
    void childCommentRejectsReplyBeyondMaximumDepth() {
        Comment depth0 = new Comment(post, userInfo, "depth-0");
        Comment depth1 = new Comment(post, depth0, userInfo, "depth-1");
        Comment depth2 = new Comment(post, depth1, userInfo, "depth-2");
        Comment depth3 = new Comment(post, depth2, userInfo, "depth-3");

        assertThatThrownBy(() ->
                new Comment(post, depth3, userInfo, "depth-4")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessage("답글을 달 수 없는 댓글입니다.");

        assertThat(depth3.getChildCount()).isZero();
        assertThat(post.getCommentCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("부모가 없는 댓글 삭제 시 공통 삭제 상태만 반영한다")
    void deletingCommentWithoutParentAppliesCommonDeletionState() {
        Comment comment = new Comment(post, userInfo, "comment");
        assertThat(comment.getMaskedContent()).isEqualTo("comment");
        assertThat(comment.isDeleted()).isFalse();

        comment.delete();

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getMaskedContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    @DisplayName("부모가 있는 댓글 삭제 시 부모의 자식 수도 추가로 감소시킨다")
    void deletingCommentWithParentAlsoUpdatesParentChildCount() {
        Comment parent = new Comment(post, userInfo, "parent");
        Comment child = new Comment(post, parent, userInfo, "child");

        child.delete();

        assertThat(parent.getChildCount()).isZero();
        assertThat(post.getCommentCount()).isEqualTo(1);
    }
}
