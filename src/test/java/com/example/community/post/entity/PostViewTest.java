package com.example.community.post.entity;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PostViewTest {

    private PostView postView;

    @BeforeEach
    void setUp() {
        UserInfo userInfo = new UserInfo(
                new SignInfo("viewer@example.com", "encoded-password"),
                "viewer",
                null
        );
        Post post = new Post(userInfo, "title", "content", null);
        postView = new PostView(post, userInfo);
    }

    @Test
    @DisplayName("마지막 조회 후 24시간이 지나지 않으면 조회를 반영하지 않는다")
    void viewReturnsFalseWithinTwentyFourHours() {
        Instant recentViewAt = Instant.now().minus(Duration.ofHours(23));
        ReflectionTestUtils.setField(postView, "viewAt", recentViewAt);

        boolean result = postView.view();

        assertThat(result).isFalse();
        assertThat(ReflectionTestUtils.getField(postView, "viewAt"))
                .isEqualTo(recentViewAt);
    }

    @Test
    @DisplayName("마지막 조회 후 24시간이 지나면 조회를 허용하고 시각을 갱신한다")
    void viewReturnsTrueAndUpdatesTimeAfterTwentyFourHours() {
        Instant oldViewAt = Instant.now().minus(Duration.ofHours(25));
        ReflectionTestUtils.setField(postView, "viewAt", oldViewAt);
        Instant beforeUpdate = Instant.now();

        boolean result = postView.view();
        Instant afterUpdate = Instant.now();
        Instant updatedViewAt = (Instant) ReflectionTestUtils.getField(
                postView,
                "viewAt"
        );

        assertThat(result).isTrue();
        assertThat(updatedViewAt).isAfter(oldViewAt);
        assertThat(updatedViewAt).isBetween(beforeUpdate, afterUpdate);
    }
}
