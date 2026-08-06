package com.example.community.post.entity;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostViewBucketTest {

    private PostViewBucket bucket;

    @BeforeEach
    void setUp() {
        UserInfo author = new UserInfo(
                new SignInfo("author@example.com", "encoded-password"),
                "author",
                null
        );
        Post post = new Post(author, "title", "content", null);
        bucket = new PostViewBucket(
                post,
                Instant.parse("2026-08-06T00:00:00Z"),
                2
        );
    }

    @Test
    @DisplayName("경계값인 증가량 0은 허용하고 기존 조회수를 유지한다")
    void increaseViewCountAcceptsZeroAmount() {
        bucket.increaseViewCount(0);

        assertThat(bucket.getViewCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("음수 증가량은 거부하고 기존 조회수를 유지한다")
    void increaseViewCountRejectsNegativeAmount() {
        assertThatThrownBy(() -> bucket.increaseViewCount(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("증가량은 0 이상이어야 합니다.");

        assertThat(bucket.getViewCount()).isEqualTo(2);
    }
}
