package com.example.community.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "PostViewBucket",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_view_bucket_post_time",
                        columnNames = {"post_num", "bucket_start_at"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_post_view_bucket_time",
                        columnList = "bucket_start_at"
                ),
                @Index(
                        name = "idx_post_view_bucket_post_time",
                        columnList = "post_num, bucket_start_at"
                )
        }
)
public class PostViewBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_view_bucket_id")
    private Long postViewBucketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_num",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_view_bucket_post")
    )
    private Post post;

    /**
     * 5분 버킷의 시작 시각.
     *
     * 예:
     * 10:00:00 이상 10:05:00 미만의 조회수라면
     * bucketStartAt은 10:00:00이다.
     */
    @Column(name = "bucket_start_at", nullable = false)
    private Instant bucketStartAt;

    /**
     * 해당 시간 버킷에서 증가한 조회수.
     */
    @Column(name = "view_count", nullable = false)
    private long viewCount;

    public PostViewBucket(
            Post post,
            Instant bucketStartAt,
            long viewCount
    ) {
        this.post = post;
        this.bucketStartAt = bucketStartAt;
        this.viewCount = viewCount;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseViewCount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("증가량은 0 이상이어야 합니다.");
        }

        this.viewCount += amount;
    }
}
