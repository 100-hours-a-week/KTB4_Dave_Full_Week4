package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.repository.PopularityAggregationCheckpointRepository;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularityCleanupServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T14:03:20Z");
    private static final Instant COMPLETED_WINDOW_END =
            Instant.parse("2026-08-02T14:00:00Z");
    private static final Duration CANDIDATE_MAX_AGE = Duration.ofHours(72);
    private static final Instant CANDIDATE_SINCE =
            NOW.minus(CANDIDATE_MAX_AGE);

    @Mock
    private PostViewBucketRepository postViewBucketRepository;

    @Mock
    private PostPopularityStatRepository postPopularityStatRepository;

    @Mock
    private PopularityAggregationCheckpointRepository checkpointRepository;

    @Mock
    private Clock clock;

    @Mock
    private PopularPostProperties popularPostProperties;

    private PopularityCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(popularPostProperties.candidateMaxAge())
                .thenReturn(CANDIDATE_MAX_AGE);
        PopularityWindowPolicy windowPolicy = new PopularityWindowPolicy(
                popularPostProperties
        );
        PopularityCheckpointLock checkpointLock =
                new PopularityCheckpointLock(checkpointRepository);
        cleanupService = new PopularityCleanupService(
                postViewBucketRepository,
                postPopularityStatRepository,
                checkpointLock,
                clock,
                windowPolicy
        );
    }












    @Test
    @DisplayName("일일 정리는 집계와 같은 체크포인트 락을 얻은 후 통계와 버킷을 삭제한다")
    void cleanupAcquiresAggregationLockBeforeDeletingExpiredData() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        when(postPopularityStatRepository
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE))
                .thenReturn(2);
        when(postViewBucketRepository
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE))
                .thenReturn(5);

        cleanupService.cleanupExpiredPopularityData();

        org.mockito.InOrder statCleanupOrder = inOrder(
                checkpointRepository,
                postPopularityStatRepository
        );
        statCleanupOrder.verify(checkpointRepository).findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        );
        statCleanupOrder.verify(postPopularityStatRepository)
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE);

        org.mockito.InOrder bucketCleanupOrder = inOrder(
                checkpointRepository,
                postViewBucketRepository
        );
        bucketCleanupOrder.verify(checkpointRepository).findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        );
        bucketCleanupOrder.verify(postViewBucketRepository)
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE);
    }

    @Test
    @DisplayName("버킷 정리가 실패하면 예외를 전파해 정리 트랜잭션을 롤백시킨다")
    void cleanupPropagatesFailureForTransactionRollback() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        when(postViewBucketRepository
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE))
                .thenThrow(new IllegalStateException("cleanup failed"));

        assertThatThrownBy(
                () -> cleanupService.cleanupExpiredPopularityData()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("cleanup failed");

        verify(postPopularityStatRepository)
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE);
    }

}
