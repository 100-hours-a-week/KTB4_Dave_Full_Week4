package com.example.community.comment.entity;

import com.example.community.post.entity.Post;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentEditRecordTest {

    private Comment comment;

    @BeforeEach
    void setUp() {
        UserInfo author = new UserInfo(
                new SignInfo("author@example.com", "encoded-password"),
                "author",
                null
        );
        Post post = new Post(author, "title", "content", null);
        comment = new Comment(post, author, "original-content");
    }

    @Test
    @DisplayName("댓글이 없으면 수정 이력을 생성할 수 없다")
    void fromRejectsNullComment() {
        assertThatThrownBy(() -> CommentEditRecord.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("comment가 null");
    }

    @Test
    @DisplayName("수정 전 댓글은 작성 시각을 이력 시각으로 사용한다")
    void fromUsesWriteTimeBeforeCommentIsEdited() {
        CommentEditRecord record = CommentEditRecord.from(comment);

        assertThat(record.getComment()).isSameAs(comment);
        assertThat(record.getVersion()).isEqualTo(comment.getVersion());
        assertThat(record.getContent()).isEqualTo("original-content");
        assertThat(record.getWriteAt()).isEqualTo(comment.getWriteAt());
    }

    @Test
    @DisplayName("수정된 댓글은 수정 시각을 이력 시각으로 사용한다")
    void fromUsesEditTimeAfterCommentIsEdited() {
        comment.update("edited-content");

        CommentEditRecord record = CommentEditRecord.from(comment);

        assertThat(record.getContent()).isEqualTo("edited-content");
        assertThat(record.getWriteAt()).isEqualTo(comment.getEditedAt());
    }

    @Test
    @DisplayName("삭제된 댓글의 수정 이력에는 마스킹된 내용을 기록한다")
    void fromUsesMaskedContentForDeletedComment() {
        comment.delete();

        CommentEditRecord record = CommentEditRecord.from(comment);

        assertThat(record.getContent()).isEqualTo("삭제된 댓글입니다.");
    }
}
