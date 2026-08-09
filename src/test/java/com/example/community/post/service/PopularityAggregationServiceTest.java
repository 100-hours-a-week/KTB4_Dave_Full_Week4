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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("최초 집계는 현재 버킷을 제외한 최근 1시간의 완료된 버킷으로 초기화한다")
    @SuppressWarnings("unchecked")
    void initialRefreshRebuildsFromCompletedBuckets() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        List<PostViewBucket> initialBuckets = List.of(
                bucket(1L, "2026-08-02T13:55:00Z", 10L),
                bucket(1L, "2026-08-02T13:30:00Z", 20L),
                bucket(1L, "2026-08-02T13:00:00Z", 5L)
        );
        when(postViewBucketRepository
                .findForPopularityRebuild(
                        Instant.parse("2026-08-02T13:00:00Z"),
                        COMPLETED_WINDOW_END,
                        CANDIDATE_SINCE
                ))
                .thenReturn(initialBuckets);
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
    @DisplayName("체크포인트가 없으면 새로 생성한 뒤 최초 집계를 완료한다")
    void initialRefreshCreatesCheckpointWhenItDoesNotExist() {
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.empty());
        when(checkpointRepository.saveAndFlush(
                any(PopularityAggregationCheckpoint.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));
        when(postViewBucketRepository.findForPopularityRebuild(
                Instant.parse("2026-08-02T13:00:00Z"),
                COMPLETED_WINDOW_END,
                CANDIDATE_SINCE
        )).thenReturn(List.of());
        ArgumentCaptor<PopularityAggregationCheckpoint> checkpointCaptor =
                ArgumentCaptor.forClass(
                        PopularityAggregationCheckpoint.class
                );

        aggregationService.refreshPopularityStats();

        verify(checkpointRepository).saveAndFlush(
                checkpointCaptor.capture()
        );
        PopularityAggregationCheckpoint createdCheckpoint =
                checkpointCaptor.getValue();
        assertThat(createdCheckpoint.getJobName())
                .isEqualTo(PopularityAggregationCheckpoint.JOB_NAME);
        assertThat(createdCheckpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }

    @Test
    @DisplayName("5분 갱신 시 새 버킷을 더하고 30분과 60분에서 만료된 버킷을 차감한다")
    void incrementalRefreshAddsAndExpiresBuckets() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));

        PostPopularityStat stat = persistedPopularityStat(1L);
        stat.initializeCounts(
                7L,
                20L,
                40L
        );
        List<PostViewBucket> relevantBuckets = List.of(
                bucket(1L, "2026-08-02T13:55:00Z", 10L),
                bucket(1L, "2026-08-02T13:25:00Z", 3L),
                bucket(1L, "2026-08-02T12:55:00Z", 2L)
        );
        when(postViewBucketRepository.findForRollingWindow(
                any(),
                any()
        ))
                .thenReturn(relevantBuckets);
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount(CANDIDATE_SINCE))
                .thenReturn(List.of(1L));
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of(stat));

        aggregationService.refreshPopularityStats();

        assertThat(stat.getViewCount5m()).isEqualTo(10L);
        assertThat(stat.getViewCount30m()).isEqualTo(27L);
        assertThat(stat.getViewCount60m()).isEqualTo(48L);
        assertThat(stat.getPopularityScore()).isEqualTo(95L);
        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }

    @Test
    @DisplayName("새 5분 버킷이 비어 있으면 직전 5분 조회수를 0으로 초기화한다")
    void incrementalRefreshClearsPreviousFiveMinuteCount() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));

        PostPopularityStat stat = persistedPopularityStat(1L);
        stat.initializeCounts(
                7L,
                20L,
                40L
        );
        when(postViewBucketRepository.findForRollingWindow(
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount(CANDIDATE_SINCE))
                .thenReturn(List.of(1L));
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of(stat));

        aggregationService.refreshPopularityStats();

        assertThat(stat.getViewCount5m()).isZero();
        assertThat(stat.getViewCount30m()).isEqualTo(20L);
        assertThat(stat.getViewCount60m()).isEqualTo(40L);
        assertThat(stat.getPopularityScore()).isEqualTo(60L);
    }

    @Test
    @DisplayName("통계가 없는 게시글에 새 5분 버킷이 생기면 인기 통계를 생성한다")
    @SuppressWarnings("unchecked")
    void incrementalRefreshCreatesStatForNewBucketWithoutExistingStat() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        PostViewBucket newBucket =
                bucket(2L, "2026-08-02T13:55:00Z", 4L);
        when(postViewBucketRepository.findForRollingWindow(
                any(),
                any()
        )).thenReturn(List.of(newBucket));
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount(CANDIDATE_SINCE))
                .thenReturn(List.of());
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of());
        ArgumentCaptor<Iterable<PostPopularityStat>> activeStatsCaptor =
                ArgumentCaptor.forClass(Iterable.class);

        aggregationService.refreshPopularityStats();

        verify(postPopularityStatRepository).saveAll(
                activeStatsCaptor.capture()
        );
        PostPopularityStat createdStat = activeStatsCaptor
                .getValue()
                .iterator()
                .next();
        assertThat(createdStat.getPost().getPostNum()).isEqualTo(2L);
        assertThat(createdStat.getViewCount5m()).isEqualTo(4L);
        assertThat(createdStat.getViewCount30m()).isEqualTo(4L);
        assertThat(createdStat.getViewCount60m()).isEqualTo(4L);
        assertThat(createdStat.getPopularityScore()).isEqualTo(16L);
        verify(postPopularityStatRepository).deleteAllByIdInBatch(
                List.of()
        );
    }

    @Test
    @DisplayName("통계가 없고 새 5분 버킷도 없으면 만료 버킷을 무시한다")
    void incrementalRefreshSkipsExpiredBucketWithoutExistingStat() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        PostViewBucket expiredBucket =
                bucket(2L, "2026-08-02T12:55:00Z", 4L);
        when(postViewBucketRepository.findForRollingWindow(
                any(),
                any()
        )).thenReturn(List.of(expiredBucket));
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount(CANDIDATE_SINCE))
                .thenReturn(List.of());
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of());

        aggregationService.refreshPopularityStats();

        verify(postPopularityStatRepository).saveAll(
                List.of()
        );
        verify(postPopularityStatRepository).deleteAllByIdInBatch(
                List.of()
        );
    }

    @Test
    @DisplayName("롤링 갱신 후 조회수가 모두 0이 된 인기 통계는 삭제한다")
    void incrementalRefreshDeletesStatWhenAllViewsExpire() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));

        PostPopularityStat stat = persistedPopularityStat(1L);
        stat.initializeCounts(0L, 0L, 4L);
        PostViewBucket expiredBucket =
                bucket(1L, "2026-08-02T12:55:00Z", 4L);
        when(postViewBucketRepository.findForRollingWindow(
                any(),
                any()
        )).thenReturn(List.of(expiredBucket));
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount(CANDIDATE_SINCE))
                .thenReturn(List.of());
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of(stat));

        aggregationService.refreshPopularityStats();

        assertThat(stat.getViewCount5m()).isZero();
        assertThat(stat.getViewCount30m()).isZero();
        assertThat(stat.getViewCount60m()).isZero();
        assertThat(stat.getPopularityScore()).isZero();
        verify(postPopularityStatRepository).saveAll(
                List.of()
        );
        verify(postPopularityStatRepository).deleteAllByIdInBatch(
                List.of(1L)
        );
    }

    @Test
    @DisplayName("변경할 인기 통계가 없어도 전체 집계 체크포인트를 전진시킨다")
    void incrementalRefreshAdvancesCheckpointWithoutAffectedStats() {
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        when(postViewBucketRepository.findForRollingWindow(
                any(),
                any()
        ))
                .thenReturn(List.of());
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount(CANDIDATE_SINCE))
                .thenReturn(List.of());

        aggregationService.refreshPopularityStats();

        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }




}
