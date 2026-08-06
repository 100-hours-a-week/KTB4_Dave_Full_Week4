package com.example.community.post.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PopularPostPropertiesTest {
    @Test
    @DisplayName("인기글 후보 기간으로 양수 Duration을 허용한다")
    void acceptsPositiveCandidateMaxAge() {
        PopularPostProperties properties = new PopularPostProperties(
                Duration.ofDays(3)
        );

        assertThat(properties.candidateMaxAge()).isEqualTo(Duration.ofHours(72));
    }

    @Test
    @DisplayName("인기글 후보 기간이 0보다 큰 최소 단위이면 설정 생성을 허용한다")
    void acceptsSmallestPositiveCandidateMaxAge() {
        Duration smallestPositiveDuration = Duration.ofNanos(1);

        PopularPostProperties properties = new PopularPostProperties(
                smallestPositiveDuration
        );

        assertThat(properties.candidateMaxAge())
                .isEqualTo(smallestPositiveDuration);
    }

    @Test
    @DisplayName("인기글 후보 기간이 0이면 설정 생성을 거부한다")
    void rejectsZeroCandidateMaxAge() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PopularPostProperties(Duration.ZERO));
    }

    @Test
    @DisplayName("인기글 후보 기간이 음수이면 설정 생성을 거부한다")
    void rejectsNegativeCandidateMaxAge() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PopularPostProperties(
                        Duration.ofSeconds(-1)
                ));
    }
}
