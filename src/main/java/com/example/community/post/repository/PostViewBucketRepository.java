package com.example.community.post.repository;

import com.example.community.post.entity.PostViewBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface PostViewBucketRepository extends JpaRepository<PostViewBucket, Long> {
    @Modifying
    @Query(
            value = """
            INSERT INTO post_view_bucket (
                post_num,
                bucket_start_at,
                view_count
            )
            SELECT
                :postNum,
                :bucketStartAt,
                :amount
            FROM post
            JOIN post_stat
              ON post_stat.post_num = post.post_num
            WHERE post.post_num = :postNum
              AND post.deleted_at IS NULL
              AND post_stat.report_count <= 5
            ON DUPLICATE KEY UPDATE
                view_count = post_view_bucket.view_count + :amount
            """,
            nativeQuery = true
    )
    int upsertViewCount(
            @Param("postNum") long postNum,
            @Param("bucketStartAt") Instant bucketStartAt,
            @Param("amount") long amount
    );

    List<PostViewBucket> findByBucketStartAtIn(Collection<Instant> bucketStartTimes);

    List<PostViewBucket> findByBucketStartAtGreaterThanEqualAndBucketStartAtLessThan(
            Instant startTime,
            Instant endTime
    );

}
