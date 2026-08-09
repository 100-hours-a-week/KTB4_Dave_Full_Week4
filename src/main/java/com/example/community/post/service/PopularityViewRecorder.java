package com.example.community.post.service;

import com.example.community.post.repository.PostViewBucketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PopularityViewRecorder {
    private final PostViewBucketRepository postViewBucketRepository;
    private final Clock clock;
    private final PopularityWindowPolicy windowPolicy;

    @Transactional
    public void recordView(long postNum, Instant writeAt) {
        Instant now = clock.instant();
        Instant candidateSince = windowPolicy.candidateSince(now);
        if (writeAt.isBefore(candidateSince)) {
            return;
        }
        postViewBucketRepository.upsertViewCount(
                postNum,
                windowPolicy.floorToBucket(now),
                1L,
                candidateSince
        );
    }
}
