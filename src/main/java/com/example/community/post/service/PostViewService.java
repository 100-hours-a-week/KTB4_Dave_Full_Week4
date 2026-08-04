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

    private void rebuildPopularityStats(Instant windowEndAt) {
        PopularityWindow window = PopularityWindow.endingAt(windowEndAt);
        List<PostViewBucket> buckets = loadBucketsForRebuild(window);
        List<PostPopularityStat> popularityStats = buildPopularityStats(
                window,
                buckets
        );
        replacePopularityStats(popularityStats);
    }

    private List<PostViewBucket> loadBucketsForRebuild(
            PopularityWindow window
    ) {
        return postViewBucketRepository
                .findByBucketStartAtGreaterThanEqualAndBucketStartAtLessThan(
                        window.start60m(),
                        window.endAt()
                );
    }

    private List<PostPopularityStat> buildPopularityStats(
            PopularityWindow window,
            List<PostViewBucket> buckets
    ) {
        Map<Long, PopularityCounts> countsByPost = new HashMap<>();
        for (PostViewBucket bucket : buckets) {
            PopularityCounts counts = countsByPost.computeIfAbsent(
                    bucket.getPost().getPostNum(),
                    ignored -> new PopularityCounts(bucket.getPost())
            );
            counts.add(bucket, window);
        }

        return countsByPost.values().stream()
                .map(PopularityCounts::toPopularityStat)
                .toList();
    }

    private void replacePopularityStats(
            List<PostPopularityStat> popularityStats
    ) {
        postPopularityStatRepository.deleteAllInBatch();
        postPopularityStatRepository.saveAll(popularityStats);
    }

    private void updateRollingWindow(Instant windowEndAt) {
        RollingWindowChanges changes = loadRollingWindowChanges(windowEndAt);
        Map<Long, PostPopularityStat> statsByPost =
                loadPopularityStats(changes.affectedPostNums());
        PopularityStatUpdates updates = applyRollingUpdates(
                changes,
                statsByPost
        );
        persistPopularityStatUpdates(updates);
    }

    private RollingWindowChanges loadRollingWindowChanges(Instant windowEndAt) {
        RollingWindowBoundaries boundaries =
                RollingWindowBoundaries.from(windowEndAt);
        RollingWindowChanges changes = new RollingWindowChanges(boundaries);
        postViewBucketRepository.findByBucketStartAtIn(boundaries.bucketStarts())
                .forEach(changes::addBucket);
        changes.addAffectedPostNums(
                postPopularityStatRepository
                        .findPostNumsWithNonZeroFiveMinuteCount()
        );
        return changes;
    }

    private Map<Long, PostPopularityStat> loadPopularityStats(
            Set<Long> affectedPostNums
    ) {
        Map<Long, PostPopularityStat> statsByPost = new HashMap<>();
        postPopularityStatRepository.findAllById(affectedPostNums)
                .forEach(stat -> statsByPost.put(stat.getPostNum(), stat));
        return statsByPost;
    }

    private PopularityStatUpdates applyRollingUpdates(
            RollingWindowChanges changes,
            Map<Long, PostPopularityStat> statsByPost
    ) {
        List<PostPopularityStat> activeStats = new ArrayList<>();
        List<Long> inactivePostNums = new ArrayList<>();

        for (Long postNum : changes.affectedPostNums()) {
            long newBucketCount = changes.newBucketCount(postNum);
            PostPopularityStat stat = statsByPost.get(postNum);

            if (stat == null) {
                if (newBucketCount == 0) {
                    continue;
                }
                stat = new PostPopularityStat(changes.post(postNum));
            }

            stat.updateRollingCounts(
                    newBucketCount,
                    changes.expired30MinuteCount(postNum),
                    changes.expired60MinuteCount(postNum)
            );

            if (stat.hasNoViews()) {
                inactivePostNums.add(postNum);
            } else {
                activeStats.add(stat);
            }
        }

        return new PopularityStatUpdates(activeStats, inactivePostNums);
    }

    private void persistPopularityStatUpdates(PopularityStatUpdates updates) {
        postPopularityStatRepository.saveAll(updates.activeStats());
        postPopularityStatRepository.deleteAllByIdInBatch(
                updates.inactivePostNums()
        );
    }

    private static class PopularityCounts {
        private final Post post;
        private long viewCount5m;
        private long viewCount30m;
        private long viewCount60m;

        private PopularityCounts(Post post) {
            this.post = post;
        }

        private void add(
                PostViewBucket bucket,
                PopularityWindow window
        ) {
            viewCount60m += bucket.getViewCount();
            if (!bucket.getBucketStartAt().isBefore(window.start30m())) {
                viewCount30m += bucket.getViewCount();
            }
            if (!bucket.getBucketStartAt().isBefore(window.start5m())) {
                viewCount5m += bucket.getViewCount();
            }
        }

        private PostPopularityStat toPopularityStat() {
            PostPopularityStat stat = new PostPopularityStat(post);
            stat.initializeCounts(
                    viewCount5m,
                    viewCount30m,
                    viewCount60m
            );
            return stat;
        }
    }

    private record PopularityWindow(
            Instant start5m,
            Instant start30m,
            Instant start60m,
            Instant endAt
    ) {
        private static PopularityWindow endingAt(Instant endAt) {
            return new PopularityWindow(
                    endAt.minus(BUCKET_DURATION),
                    endAt.minus(Duration.ofMinutes(30)),
                    endAt.minus(POPULARITY_WINDOW),
                    endAt
            );
        }
    }

    private record RollingWindowBoundaries(
            Instant newBucketStartAt,
            Instant expired30MinuteBucketStartAt,
            Instant expired60MinuteBucketStartAt
    ) {
        private static RollingWindowBoundaries from(Instant windowEndAt) {
            return new RollingWindowBoundaries(
                    windowEndAt.minus(BUCKET_DURATION),
                    windowEndAt.minus(Duration.ofMinutes(35)),
                    windowEndAt.minus(Duration.ofMinutes(65))
            );
        }

        private List<Instant> bucketStarts() {
            return List.of(
                    newBucketStartAt,
                    expired30MinuteBucketStartAt,
                    expired60MinuteBucketStartAt
            );
        }
    }

    private static class RollingWindowChanges {
        private final RollingWindowBoundaries boundaries;
        private final Map<Long, Post> postsByPostNum = new HashMap<>();
        private final Map<Long, Long> newBucketCounts = new HashMap<>();
        private final Map<Long, Long> expired30MinuteCounts = new HashMap<>();
        private final Map<Long, Long> expired60MinuteCounts = new HashMap<>();
        private final Set<Long> affectedPostNums = new HashSet<>();

        private RollingWindowChanges(RollingWindowBoundaries boundaries) {
            this.boundaries = boundaries;
        }

        private void addBucket(PostViewBucket bucket) {
            Long postNum = bucket.getPost().getPostNum();
            postsByPostNum.put(postNum, bucket.getPost());
            affectedPostNums.add(postNum);

            if (bucket.getBucketStartAt().equals(
                    boundaries.newBucketStartAt()
            )) {
                newBucketCounts.put(postNum, bucket.getViewCount());
                return;
            }
            if (bucket.getBucketStartAt().equals(
                    boundaries.expired30MinuteBucketStartAt()
            )) {
                expired30MinuteCounts.put(postNum, bucket.getViewCount());
                return;
            }
            expired60MinuteCounts.put(postNum, bucket.getViewCount());
        }

        private void addAffectedPostNums(Collection<Long> postNums) {
            affectedPostNums.addAll(postNums);
        }

        private Set<Long> affectedPostNums() {
            return affectedPostNums;
        }

        private Post post(Long postNum) {
            return postsByPostNum.get(postNum);
        }

        private long newBucketCount(Long postNum) {
            return newBucketCounts.getOrDefault(postNum, 0L);
        }

        private long expired30MinuteCount(Long postNum) {
            return expired30MinuteCounts.getOrDefault(postNum, 0L);
        }

        private long expired60MinuteCount(Long postNum) {
            return expired60MinuteCounts.getOrDefault(postNum, 0L);
        }
    }

    private record PopularityStatUpdates(
            List<PostPopularityStat> activeStats,
            List<Long> inactivePostNums
    ) {
    }
}
