package com.example.community.post.service;

import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopularityCleanupService {
    private final PostViewBucketRepository postViewBucketRepository;
    private final PostPopularityStatRepository postPopularityStatRepository;
    private final PopularityCheckpointLock checkpointLock;
    private final Clock clock;
    private final PopularityWindowPolicy windowPolicy;

    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredPopularityData() {
        checkpointLock.acquire();
        Instant candidateSince = windowPolicy.candidateSince(clock.instant());
        int deletedStatCount = postPopularityStatRepository
                .deleteAllByPostWriteAtBefore(candidateSince);
        int deletedBucketCount = postViewBucketRepository
                .deleteAllByPostWriteAtBefore(candidateSince);

        log.info(
                "오래된 인기글 데이터를 정리했습니다. candidateSince={}, "
                        + "deletedStats={}, deletedBuckets={}",
                candidateSince,
                deletedStatCount,
                deletedBucketCount
        );
    }
}
