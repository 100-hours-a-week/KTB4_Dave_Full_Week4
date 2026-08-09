package com.example.community.post.service;

import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import com.example.community.post.service.PopularityWindowPolicy.PopularityWindow;
import com.example.community.post.service.PopularityWindowPolicy.RollingWindowBoundaries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PopularityAggregationService {
    private final PostViewBucketRepository postViewBucketRepository;
    private final PostPopularityStatRepository postPopularityStatRepository;
    private final PopularityCheckpointLock checkpointLock;
    private final Clock clock;
    private final PopularityWindowPolicy windowPolicy;

    @Transactional
    public void refreshPopularityStats() {
        Instant now = clock.instant();
        Instant targetEndAt = windowPolicy.floorToBucket(now);
        Instant candidateSince = windowPolicy.candidateSince(now);
        PopularityAggregationCheckpoint checkpoint = checkpointLock.acquire();

        if (checkpoint.getLastProcessedEndAt() == null) {
            rebuildPopularityStats(targetEndAt, candidateSince);
            checkpoint.advanceTo(targetEndAt);
            return;
        }

        Instant nextEndAt = windowPolicy.nextWindowEnd(
                checkpoint.getLastProcessedEndAt()
        );
        while (!nextEndAt.isAfter(targetEndAt)) {
            updateRollingWindow(nextEndAt, candidateSince);
            checkpoint.advanceTo(nextEndAt);
            nextEndAt = windowPolicy.nextWindowEnd(nextEndAt);
        }
    }


    private void rebuildPopularityStats(
            Instant windowEndAt,
            Instant candidateSince
    ) {
        PopularityWindow window = windowPolicy.windowEndingAt(windowEndAt);
        List<PostViewBucket> buckets = loadBucketsForRebuild(
                window,
                candidateSince
        );
        List<PostPopularityStat> popularityStats = buildPopularityStats(
                window,
                buckets
        );
        replacePopularityStats(popularityStats);
    }

    private List<PostViewBucket> loadBucketsForRebuild(
            PopularityWindow window,
            Instant candidateSince
    ) {
        return postViewBucketRepository
                .findForPopularityRebuild(
                        window.start60m(),
                        window.endAt(),
                        candidateSince
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

    private void updateRollingWindow(
            Instant windowEndAt,
            Instant candidateSince
    ) {
        RollingWindowChanges changes = loadRollingWindowChanges(
                windowEndAt,
                candidateSince
        );
        Map<Long, PostPopularityStat> statsByPost =
                loadPopularityStats(changes.affectedPostNums());
        PopularityStatUpdates updates = applyRollingUpdates(
                changes,
                statsByPost
        );
        persistPopularityStatUpdates(updates);
    }

    private RollingWindowChanges loadRollingWindowChanges(
            Instant windowEndAt,
            Instant candidateSince
    ) {
        RollingWindowBoundaries boundaries =
                windowPolicy.rollingWindowEndingAt(windowEndAt);
        RollingWindowChanges changes = new RollingWindowChanges(boundaries);
        postViewBucketRepository.findForRollingWindow(
                        boundaries.bucketStarts(),
                        candidateSince
                )
                .forEach(changes::addBucket);
        changes.addAffectedPostNums(
                postPopularityStatRepository
                        .findPostNumsWithNonZeroFiveMinuteCount(candidateSince)
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
