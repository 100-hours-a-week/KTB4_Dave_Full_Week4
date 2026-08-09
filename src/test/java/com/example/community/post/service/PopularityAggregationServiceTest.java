package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.post.repository.PopularityAggregationCheckpointRepository;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.community.post.fixture.PopularityTestFixture.bucket;
import static com.example.community.post.fixture.PopularityTestFixture.persistedPopularityStat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularityAggregationServiceTest {
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

    private PopularityAggregationService aggregationService;

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
        aggregationService = new PopularityAggregationService(
                postViewBucketRepository,
                postPopularityStatRepository,
                checkpointLock,
                clock,
                windowPolicy
        );
    }

    @Test
    @DisplayName("최초 집계는 현재 버킷을 제외한 최근 1시간 창 전체를 합산한다")
    @SuppressWarnings("unchecked")
    void initialRefreshRebuildsFromCompletedBuckets() {
        PopularityAggregationCheckpoint checkpoint = checkpoint(null);
        List<PostViewBucket> buckets = List.of(
                bucket(1L, "2026-08-02T13:55:00Z", 10L),
                bucket(1L, "2026-08-02T13:30:00Z", 20L),
                bucket(1L, "2026-08-02T13:00:00Z", 5L)
        );
        when(postViewBucketRepository.findForPopularityRebuild(
                Instant.parse("2026-08-02T13:00:00Z"),
                COMPLETED_WINDOW_END,
                CANDIDATE_SINCE
        )).thenReturn(buckets);
        ArgumentCaptor<Iterable<PostPopularityStat>> captor =
                ArgumentCaptor.forClass(Iterable.class);

        aggregationService.refreshPopularityStats();

        verify(postPopularityStatRepository).saveAll(captor.capture());
        PostPopularityStat stat = captor.getValue().iterator().next();
        assertThat(stat.getViewCount5m()).isEqualTo(10L);
        assertThat(stat.getViewCount30m()).isEqualTo(30L);
        assertThat(stat.getViewCount60m()).isEqualTo(35L);
        assertThat(stat.getPopularityScore()).isEqualTo(85L);
        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }

    @Test
    @DisplayName("재집계 범위를 벗어난 버킷은 거부하고 체크포인트를 전진시키지 않는다")
    void refreshRejectsBucketOutsideRebuildWindow() {
        PopularityAggregationCheckpoint checkpoint = checkpoint(null);
        PostViewBucket outOfRangeBucket =
                bucket(1L, "2026-08-02T12:55:00Z", 1L);
        when(postViewBucketRepository.findForPopularityRebuild(
                Instant.parse("2026-08-02T13:00:00Z"),
                COMPLETED_WINDOW_END,
                CANDIDATE_SINCE
        )).thenReturn(List.of(outOfRangeBucket));

        assertThatThrownBy(aggregationService::refreshPopularityStats)
                .isInstanceOf(IllegalStateException.class);

        assertThat(checkpoint.getLastProcessedEndAt()).isNull();
        verifyNoInteractions(postPopularityStatRepository);
    }

    @Test
    @DisplayName("체크포인트가 없으면 생성한 뒤 최초 집계를 완료한다")
    void initialRefreshCreatesCheckpointWhenItDoesNotExist() {
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.empty());
        when(checkpointRepository.saveAndFlush(
                any(PopularityAggregationCheckpoint.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));
        when(postViewBucketRepository.findForPopularityRebuild(
                any(),
                any(),
                any()
        )).thenReturn(List.of());
        ArgumentCaptor<PopularityAggregationCheckpoint> captor =
                ArgumentCaptor.forClass(
                        PopularityAggregationCheckpoint.class
                );

        aggregationService.refreshPopularityStats();

        verify(checkpointRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }

    @Test
    @DisplayName("매 5분 갱신은 증분 차감 대신 현재 60분 창 전체를 다시 합산한다")
    void scheduledRefreshRebuildsCurrentWindow() {
        PopularityAggregationCheckpoint checkpoint = checkpoint(
                Instant.parse("2026-08-02T13:55:00Z")
        );
        PostPopularityStat existingStat = persistedPopularityStat(1L);
        existingStat.initializeCounts(1L, 2L, 3L);
        PostViewBucket rebuiltBucket =
                bucket(1L, "2026-08-02T13:55:00Z", 4L);
        when(postPopularityStatRepository.findAll())
                .thenReturn(List.of(existingStat));
        when(postViewBucketRepository.findForPopularityRebuild(
                Instant.parse("2026-08-02T13:00:00Z"),
                COMPLETED_WINDOW_END,
                CANDIDATE_SINCE
        )).thenReturn(List.of(rebuiltBucket));

        aggregationService.refreshPopularityStats();

        assertThat(existingStat.getViewCount5m()).isEqualTo(4L);
        assertThat(existingStat.getViewCount30m()).isEqualTo(4L);
        assertThat(existingStat.getViewCount60m()).isEqualTo(4L);
        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
        verify(postViewBucketRepository, never())
                .findForRollingWindow(any(), any());
    }

    @Test
    @DisplayName("경계 뒤 늦게 커밋되어 증가한 완료 버킷 값도 다음 재집계에 반영한다")
    void nextRefreshIncludesLateCommittedBucketIncrement() {
        Instant nextNow = Instant.parse("2026-08-02T14:08:20Z");
        when(clock.instant()).thenReturn(NOW, nextNow);
        PopularityAggregationCheckpoint checkpoint = checkpoint(
                Instant.parse("2026-08-02T13:55:00Z")
        );
        PostPopularityStat existingStat = persistedPopularityStat(1L);
        PostViewBucket firstSnapshot =
                bucket(1L, "2026-08-02T13:55:00Z", 10L);
        PostViewBucket lateCommitSnapshot =
                bucket(1L, "2026-08-02T13:55:00Z", 11L);
        when(postPopularityStatRepository.findAll())
                .thenReturn(List.of(existingStat));
        when(postViewBucketRepository.findForPopularityRebuild(
                any(),
                any(),
                any()
        )).thenReturn(List.of(firstSnapshot), List.of(lateCommitSnapshot));

        aggregationService.refreshPopularityStats();
        assertThat(existingStat.getViewCount5m()).isEqualTo(10L);

        aggregationService.refreshPopularityStats();

        assertThat(existingStat.getViewCount5m()).isZero();
        assertThat(existingStat.getViewCount30m()).isEqualTo(11L);
        assertThat(existingStat.getViewCount60m()).isEqualTo(11L);
        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(Instant.parse("2026-08-02T14:05:00Z"));
    }

    @Test
    @DisplayName("집계가 여러 회차 밀려도 현재 창을 한 번만 재집계한다")
    void delayedRefreshRebuildsOnlyOnce() {
        PopularityAggregationCheckpoint checkpoint = checkpoint(
                Instant.parse("2026-08-02T13:00:00Z")
        );
        when(postViewBucketRepository.findForPopularityRebuild(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        aggregationService.refreshPopularityStats();

        verify(postViewBucketRepository, times(1))
                .findForPopularityRebuild(any(), any(), any());
        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }

    @Test
    @DisplayName("현재 5분 경계를 이미 처리했다면 다시 집계하지 않는다")
    void refreshDoesNothingForProcessedWindow() {
        checkpoint(COMPLETED_WINDOW_END);

        aggregationService.refreshPopularityStats();

        verifyNoInteractions(
                postViewBucketRepository,
                postPopularityStatRepository
        );
    }

    private PopularityAggregationCheckpoint checkpoint(Instant processedAt) {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        if (processedAt != null) {
            checkpoint.advanceTo(processedAt);
        }
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        return checkpoint;
    }
}
