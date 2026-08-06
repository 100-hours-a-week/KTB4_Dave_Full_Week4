package com.example.community.post.entity;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostPopularityStatTest {

    private PostPopularityStat stat;

    @BeforeEach
    void setUp() {
        UserInfo author = new UserInfo(
                new SignInfo("author@example.com", "encoded-password"),
                "author",
                null
        );
        stat = new PostPopularityStat(
                new Post(author, "title", "content", null)
        );
    }

    @Test
    @DisplayName("초기 조회수에 시간 구간별 가중치를 적용해 인기 점수를 계산한다")
    void initializeCountsCalculatesWeightedPopularityScore() {
        stat.initializeCounts(3, 5, 8);

        assertThat(stat.getViewCount5m()).isEqualTo(3);
        assertThat(stat.getViewCount30m()).isEqualTo(5);
        assertThat(stat.getViewCount60m()).isEqualTo(8);
        assertThat(stat.getPopularityScore()).isEqualTo(19);
        assertThat(stat.hasNoViews()).isFalse();
    }

    @Test
    @DisplayName("60분 조회수가 0인 경우에만 조회수 없음으로 판단한다")
    void hasNoViewsDependsOnSixtyMinuteCount() {
        stat.initializeCounts(0, 0, 0);
        assertThat(stat.hasNoViews()).isTrue();

        stat.initializeCounts(0, 0, 1);
        assertThat(stat.hasNoViews()).isFalse();
    }

    @Test
    @DisplayName("새 버킷을 더하고 만료 버킷을 차감해 롤링 조회수와 점수를 갱신한다")
    void updateRollingCountsAddsNewAndSubtractsExpiredBuckets() {
        stat.initializeCounts(2, 10, 20);

        stat.updateRollingCounts(4, 3, 5);

        assertThat(stat.getViewCount5m()).isEqualTo(4);
        assertThat(stat.getViewCount30m()).isEqualTo(11);
        assertThat(stat.getViewCount60m()).isEqualTo(19);
        assertThat(stat.getPopularityScore()).isEqualTo(38);
    }

    @Test
    @DisplayName("만료량과 현재량이 같으면 롤링 조회수를 0으로 갱신한다")
    void updateRollingCountsAllowsZeroResult() {
        stat.initializeCounts(0, 1, 1);

        stat.updateRollingCounts(1, 2, 2);

        assertThat(stat.getViewCount30m()).isZero();
        assertThat(stat.getViewCount60m()).isZero();
        assertThat(stat.getPopularityScore()).isEqualTo(2);
    }

    @Test
    @DisplayName("30분 만료량 차감 결과가 음수면 갱신을 거부한다")
    void updateRollingCountsRejectsNegativeThirtyMinuteResult() {
        stat.initializeCounts(0, 1, 10);

        assertThatThrownBy(() -> stat.updateRollingCounts(1, 3, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("인기글 조회수 집계 결과는 음수가 될 수 없습니다.");
    }

    @Test
    @DisplayName("60분 만료량 차감 결과가 음수면 갱신을 거부한다")
    void updateRollingCountsRejectsNegativeSixtyMinuteResult() {
        stat.initializeCounts(0, 10, 1);

        assertThatThrownBy(() -> stat.updateRollingCounts(1, 0, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("인기글 조회수 집계 결과는 음수가 될 수 없습니다.");
    }
}
