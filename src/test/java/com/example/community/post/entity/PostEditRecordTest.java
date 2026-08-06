package com.example.community.post.entity;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostEditRecordTest {

    private Post post;

    @BeforeEach
    void setUp() {
        UserInfo author = new UserInfo(
                new SignInfo("author@example.com", "encoded-password"),
                "author",
                null
        );
        post = new Post(author, "title", "content", "posts/image.png");
    }

    @Test
    @DisplayName("게시글이 없으면 수정 이력을 생성할 수 없다")
    void constructorRejectsNullPost() {
        assertThatThrownBy(() -> new PostEditRecord(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("post가 null");
    }

    @Test
    @DisplayName("수정 전 게시글은 작성 시각을 이력 시각으로 사용한다")
    void constructorUsesWriteTimeBeforePostIsEdited() {
        PostEditRecord record = new PostEditRecord(post);

        assertThat(record.getPost()).isSameAs(post);
        assertThat(record.getVersion()).isEqualTo(post.getVersion());
        assertThat(record.getTitle()).isEqualTo("title");
        assertThat(record.getContent()).isEqualTo("content");
        assertThat(record.getImage()).isEqualTo("posts/image.png");
        assertThat(record.getWriteAt()).isEqualTo(post.getWriteAt());
    }

    @Test
    @DisplayName("수정된 게시글은 수정 시각을 이력 시각으로 사용한다")
    void constructorUsesEditTimeAfterPostIsEdited() {
        post.update("edited-title", "edited-content", null);

        PostEditRecord record = new PostEditRecord(post);

        assertThat(record.getTitle()).isEqualTo("edited-title");
        assertThat(record.getContent()).isEqualTo("edited-content");
        assertThat(record.getWriteAt()).isEqualTo(post.getEditedAt());
    }
}
