package com.example.community.post.service;

import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import com.example.community.post.service.PopularityWindowPolicy.PopularityWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
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
        Instant lastProcessedEndAt = checkpoint.getLastProcessedEndAt();

        if (lastProcessedEndAt != null
                && !lastProcessedEndAt.isBefore(targetEndAt)) {
            return;
        }
        if (lastProcessedEndAt != null
                && windowPolicy.nextWindowEnd(lastProcessedEndAt)
                .isBefore(targetEndAt)) {
            log.warn(
                    "인기글 집계가 밀려 현재 60분 창을 한 번 재집계합니다: "
                            + "lastProcessedEndAt={}, targetEndAt={}",
                    lastProcessedEndAt,
                    targetEndAt
            );
        }

        rebuildPopularityStats(targetEndAt, candidateSince);
        checkpoint.advanceTo(targetEndAt);
    }

    private void rebuildPopularityStats(
            Instant windowEndAt,
            Instant candidateSince
    ) {
        PopularityWindow window = windowPolicy.windowEndingAt(windowEndAt);
        List<PostViewBucket> buckets = postViewBucketRepository
                .findForPopularityRebuild(
                        window.start60m(),
                        window.endAt(),
                        candidateSince
                );
        replacePopularityStats(buildPopularityStats(window, buckets));
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
            List<PostPopularityStat> rebuiltStats
    ) {
        Map<Long, PostPopularityStat> existingStatsByPostNum = new HashMap<>();
        postPopularityStatRepository.findAll().forEach(stat ->
                existingStatsByPostNum.put(stat.getPostNum(), stat)
        );

        List<PostPopularityStat> activeStats = new ArrayList<>();
        for (PostPopularityStat rebuiltStat : rebuiltStats) {
            Long postNum = rebuiltStat.getPost().getPostNum();
            PostPopularityStat existingStat =
                    existingStatsByPostNum.remove(postNum);
            if (existingStat == null) {
                activeStats.add(rebuiltStat);
                continue;
            }
            existingStat.initializeCounts(
                    rebuiltStat.getViewCount5m(),
                    rebuiltStat.getViewCount30m(),
                    rebuiltStat.getViewCount60m()
            );
            activeStats.add(existingStat);
        }

        postPopularityStatRepository.saveAll(activeStats);
        postPopularityStatRepository.deleteAllByIdInBatch(
                existingStatsByPostNum.keySet()
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

        private void add(PostViewBucket bucket, PopularityWindow window) {
            Instant bucketStartAt = bucket.getBucketStartAt();
            if (bucketStartAt.isBefore(window.start60m())
                    || !bucketStartAt.isBefore(window.endAt())) {
                throw new IllegalStateException(
                        "인기글 재집계 범위를 벗어난 버킷입니다: "
                                + bucketStartAt
                );
            }

            viewCount60m += bucket.getViewCount();
            if (!bucketStartAt.isBefore(window.start30m())) {
                viewCount30m += bucket.getViewCount();
            }
            if (!bucketStartAt.isBefore(window.start5m())) {
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
}
