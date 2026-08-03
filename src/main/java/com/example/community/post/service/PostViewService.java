package com.example.community.post.service;

import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.post.repository.PopularityAggregationCheckpointRepository;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PostViewService {
    private static final Duration BUCKET_DURATION = Duration.ofMinutes(5);
    private static final Duration POPULARITY_WINDOW = Duration.ofMinutes(60);
    private static final int POPULAR_POST_LIMIT = 10;

    private final PostViewBucketRepository postViewBucketRepository;
    private final PostPopularityStatRepository postPopularityStatRepository;
    private final PopularityAggregationCheckpointRepository checkpointRepository;
    private final Clock clock;

    @Transactional
    public void recordView(long postNum) {
        Instant bucketStartAt = floorToFiveMinutes(clock.instant());
        postViewBucketRepository.upsertViewCount(postNum, bucketStartAt, 1L);
    }

    @Transactional
    public void refreshPopularityStats() {
        Instant targetEndAt = floorToFiveMinutes(clock.instant());
        PopularityAggregationCheckpoint checkpoint = lockCheckpoint();

        if (checkpoint.getLastProcessedEndAt() == null) {
            rebuildPopularityStats(targetEndAt);
            checkpoint.advanceTo(targetEndAt);
            return;
        }

        Instant nextEndAt = checkpoint.getLastProcessedEndAt().plus(BUCKET_DURATION);
        while (!nextEndAt.isAfter(targetEndAt)) {
            updateRollingWindow(nextEndAt);
            checkpoint.advanceTo(nextEndAt);
            nextEndAt = nextEndAt.plus(BUCKET_DURATION);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getTop10PopularPostNums() {
        return postPopularityStatRepository.findPopularPostNums(
                PageRequest.of(0, POPULAR_POST_LIMIT)
        );
    }

    Instant floorToFiveMinutes(Instant instant) {
        long bucketSeconds = BUCKET_DURATION.toSeconds();
        long flooredEpochSecond = Math.floorDiv(
                instant.getEpochSecond(),
                bucketSeconds
        ) * bucketSeconds;
        return Instant.ofEpochSecond(flooredEpochSecond);
    }

    private PopularityAggregationCheckpoint lockCheckpoint() {
        return checkpointRepository
                .findByJobNameForUpdate(PopularityAggregationCheckpoint.JOB_NAME)
                .orElseGet(() -> checkpointRepository.saveAndFlush(
                        new PopularityAggregationCheckpoint(
                                PopularityAggregationCheckpoint.JOB_NAME
                        )
                ));
    }

    private void rebuildPopularityStats(Instant targetEndAt) {
        Instant startAt = targetEndAt.minus(POPULARITY_WINDOW);
        Instant fiveMinuteStartAt = targetEndAt.minus(BUCKET_DURATION);
        Instant thirtyMinuteStartAt = targetEndAt.minus(Duration.ofMinutes(30));
        List<PostViewBucket> buckets = postViewBucketRepository
                .findByBucketStartAtGreaterThanEqualAndBucketStartAtLessThan(
                        startAt,
                        targetEndAt
                );

        Map<Long, PopularityCounts> countsByPost = new HashMap<>();
        for (PostViewBucket bucket : buckets) {
            PopularityCounts counts = countsByPost.computeIfAbsent(
                    bucket.getPost().getPostNum(),
                    ignored -> new PopularityCounts(bucket.getPost())
            );
            counts.viewCount60m += bucket.getViewCount();
            if (!bucket.getBucketStartAt().isBefore(thirtyMinuteStartAt)) {
                counts.viewCount30m += bucket.getViewCount();
            }
            if (!bucket.getBucketStartAt().isBefore(fiveMinuteStartAt)) {
                counts.viewCount5m += bucket.getViewCount();
            }
        }

        List<PostPopularityStat> popularityStats = countsByPost.entrySet().stream()
                .map(entry -> {
                    PopularityCounts counts = entry.getValue();
                    PostPopularityStat stat = new PostPopularityStat(counts.post);
                    stat.initializeCounts(
                            counts.viewCount5m,
                            counts.viewCount30m,
                            counts.viewCount60m
                    );
                    return stat;
                })
                .toList();

        postPopularityStatRepository.deleteAllInBatch();
        postPopularityStatRepository.saveAll(popularityStats);
    }

    private void updateRollingWindow(Instant windowEndAt) {
        Instant newBucketStartAt = windowEndAt.minus(BUCKET_DURATION);
        Instant expired30MinuteBucketStartAt = windowEndAt.minus(Duration.ofMinutes(35));
        Instant expired60MinuteBucketStartAt = windowEndAt.minus(Duration.ofMinutes(65));
        List<Instant> relevantBucketStarts = List.of(
                newBucketStartAt,
                expired30MinuteBucketStartAt,
                expired60MinuteBucketStartAt
        );

        Map<Instant, Map<Long, Long>> countsByTimeAndPost = new HashMap<>();
        Map<Long, Post> postsByPostNum = new HashMap<>();
        postViewBucketRepository.findByBucketStartAtIn(relevantBucketStarts)
                .forEach(bucket -> {
                    Long postNum = bucket.getPost().getPostNum();
                    postsByPostNum.put(postNum, bucket.getPost());
                    countsByTimeAndPost
                            .computeIfAbsent(
                                    bucket.getBucketStartAt(),
                                    ignored -> new HashMap<>()
                            )
                            .put(postNum, bucket.getViewCount());
                });

        Map<Long, Long> newBucketCounts = countsByTimeAndPost.getOrDefault(
                newBucketStartAt,
                Map.of()
        );
        Map<Long, Long> expired30MinuteCounts = countsByTimeAndPost.getOrDefault(
                expired30MinuteBucketStartAt,
                Map.of()
        );
        Map<Long, Long> expired60MinuteCounts = countsByTimeAndPost.getOrDefault(
                expired60MinuteBucketStartAt,
                Map.of()
        );

        Set<Long> affectedPostNums = new HashSet<>(
                postPopularityStatRepository
                        .findPostNumsWithNonZeroFiveMinuteCount()
        );
        affectedPostNums.addAll(newBucketCounts.keySet());
        affectedPostNums.addAll(expired30MinuteCounts.keySet());
        affectedPostNums.addAll(expired60MinuteCounts.keySet());

        Map<Long, PostPopularityStat> statsByPost = new HashMap<>();
        postPopularityStatRepository.findAllById(affectedPostNums)
                .forEach(stat -> statsByPost.put(stat.getPostNum(), stat));

        List<PostPopularityStat> updatedStats = new ArrayList<>();
        List<Long> inactivePostNums = new ArrayList<>();

        for (Long postNum : affectedPostNums) {
            long newBucketCount = newBucketCounts.getOrDefault(postNum, 0L);
            PostPopularityStat stat = statsByPost.get(postNum);

            if (stat == null) {
                if (newBucketCount == 0) {
                    continue;
                }
                stat = new PostPopularityStat(postsByPostNum.get(postNum));
            }

            stat.updateRollingCounts(
                    newBucketCount,
                    expired30MinuteCounts.getOrDefault(postNum, 0L),
                    expired60MinuteCounts.getOrDefault(postNum, 0L)
            );

            if (stat.hasNoViews()) {
                inactivePostNums.add(postNum);
            } else {
                updatedStats.add(stat);
            }
        }

        postPopularityStatRepository.saveAll(updatedStats);
        postPopularityStatRepository.deleteAllByIdInBatch(inactivePostNums);
    }

    private static class PopularityCounts {
        private final Post post;
        private long viewCount5m;
        private long viewCount30m;
        private long viewCount60m;

        private PopularityCounts(Post post) {
            this.post = post;
        }
    }
}
