package com.example.community.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "PostPopularityStat",
        indexes = @Index(
                name = "idx_post_popularity_score",
                columnList = "popularityScore DESC, viewCount5m DESC, " +
                        "viewCount30m DESC, postNum DESC"
        )
)
public class PostPopularityStat {
    @Id
    @Column(name = "postNum")
    private Long postNum;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "postNum",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_post_popularity_stat_post"
            )
    )
    private Post post;

    @Column(name = "viewCount5m", nullable = false)
    private long viewCount5m;

    @Column(name = "viewCount30m", nullable = false)
    private long viewCount30m;

    @Column(name = "viewCount60m", nullable = false)
    private long viewCount60m;

    @Column(name = "popularityScore", nullable = false)
    private long popularityScore;

    public PostPopularityStat(Post post) {
        this.post = post;
    }

    public void updateRollingCounts(
            long newBucketCount,
            long expired30MinuteCount,
            long expired60MinuteCount
    ) {
        this.viewCount5m = newBucketCount;
        this.viewCount30m = subtractExpired(
                viewCount30m + newBucketCount,
                expired30MinuteCount
        );
        this.viewCount60m = subtractExpired(
                viewCount60m + newBucketCount,
                expired60MinuteCount
        );
        updatePopularityScore();
    }

    public void initializeCounts(
            long viewCount5m,
            long viewCount30m,
            long viewCount60m
    ) {
        this.viewCount5m = viewCount5m;
        this.viewCount30m = viewCount30m;
        this.viewCount60m = viewCount60m;
        updatePopularityScore();
    }

    public boolean hasNoViews() {
        return viewCount60m == 0;
    }

    private long subtractExpired(long currentCount, long expiredCount) {
        long result = currentCount - expiredCount;
        if (result < 0) {
            throw new IllegalStateException("인기글 조회수 집계 결과는 음수가 될 수 없습니다.");
        }
        return result;
    }

    private void updatePopularityScore() {
        popularityScore = viewCount5m * 2 + viewCount30m + viewCount60m;
    }
}
