package com.example.community.user.entity;

import com.example.community.post.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserLikePostTest {

    private UserInfo userInfo;
    private Post post;

    @BeforeEach
    void setUp() {
        userInfo = new UserInfo(
                new SignInfo("user@example.com", "encoded-password"),
                "user",
                null
        );
        post = new Post(userInfo, "title", "content", null);
    }

    @Test
    @DisplayName("사용자나 게시글이 없으면 좋아요를 생성할 수 없다")
    void constructorRejectsNullRequiredArguments() {
        assertThatThrownBy(() -> new UserLikePost(null, post))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("null이 아닌 인자가 전달돼야 함");
        assertThatThrownBy(() -> new UserLikePost(userInfo, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("null이 아닌 인자가 전달돼야 함");

        assertThat(post.getPostState().getLikeCount()).isZero();
    }

    @Test
    @DisplayName("좋아요를 생성하면 게시글 좋아요 수를 증가시킨다")
    void constructorIncreasesPostLikeCount() {
        UserLikePost like = new UserLikePost(userInfo, post);

        assertThat(like.getUserInfo()).isSameAs(userInfo);
        assertThat(like.getPost()).isSameAs(post);
        assertThat(post.getPostState().getLikeCount()).isEqualTo(1);
    }
}
