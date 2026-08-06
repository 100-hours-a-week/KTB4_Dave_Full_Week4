package com.example.community.post.entity;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("신고가 5건 이하면 원래 제목을 노출하고 6건부터 제목을 마스킹한다")
    void maskedTitleChangesAfterReportThresholdIsExceeded() {
        Post post = post();

        assertThat(post.isBlind()).isFalse();
        assertThat(post.getMaskedTitle()).isEqualTo("title");

        for (int count = 0; count < 5; count++) {
            post.report();
        }

        assertThat(post.getPostState().getReportCount()).isEqualTo(5);
        assertThat(post.isBlind()).isFalse();
        assertThat(post.getMaskedTitle()).isEqualTo("title");

        assertThat(post.report()).isEqualTo(6);
        assertThat(post.isBlind()).isTrue();
        assertThat(post.getMaskedTitle()).isEqualTo("신고 처리된 글");
    }

    private Post post() {
        UserInfo author = new UserInfo(
                new SignInfo("author@example.com", "encoded-password"),
                "author",
                null
        );
        return new Post(author, "title", "content", null);
    }
}
