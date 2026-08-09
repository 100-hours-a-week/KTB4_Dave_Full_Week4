package com.example.community.post.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostSortPolicyTest {

    @Test
    @DisplayName("latest 정렬은 게시글 번호 역순을 사용한다")
    void latestUsesPostNumberDescending() {
        assertThat(PostSortPolicy.forPosts("latest").getOrderFor("postNum"))
                .isNotNull()
                .satisfies(order -> assertThat(order.isDescending()).isTrue());
    }

    @Test
    @DisplayName("지원하지 않는 정렬 기준은 거부한다")
    void rejectsUnsupportedSort() {
        assertThatThrownBy(() -> PostSortPolicy.forPosts("like"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 정렬 기준입니다: like");
    }

    @Test
    @DisplayName("정렬 기준이 null이면 거부한다")
    void rejectsNullSort() {
        assertThatThrownBy(() -> PostSortPolicy.forLikedPosts(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("정렬 기준은 필수입니다.");
    }
}
