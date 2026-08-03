package com.example.community.post.service;

import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.post.repository.PopularityAggregationCheckpointRepository;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostViewServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T14:03:20Z");
    private static final Instant COMPLETED_WINDOW_END =
            Instant.parse("2026-08-02T14:00:00Z");

    @Mock
    private PostViewBucketRepository postViewBucketRepository;

    @Mock
    private PostPopularityStatRepository postPopularityStatRepository;

    @Mock
    private PopularityAggregationCheckpointRepository checkpointRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private PostViewService postViewService;

    @Test
    @DisplayName("조회수를 현재 시각이 속한 5분 버킷에 기록한다")
    void recordViewUsesFlooredFiveMinuteBucket() {
        when(clock.instant()).thenReturn(NOW);

        postViewService.recordView(1L);

        verify(postViewBucketRepository).upsertViewCount(
                1L,
                COMPLETED_WINDOW_END,
                1L
        );
    }

    @Test
    @DisplayName("최초 집계는 현재 버킷을 제외한 최근 1시간의 완료된 버킷으로 초기화한다")
    @SuppressWarnings("unchecked")
    void initialRefreshRebuildsFromCompletedBuckets() {
        when(clock.instant()).thenReturn(NOW);
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
                .findByBucketStartAtGreaterThanEqualAndBucketStartAtLessThan(
                        Instant.parse("2026-08-02T13:00:00Z"),
                        COMPLETED_WINDOW_END
                ))
                .thenReturn(initialBuckets);
        ArgumentCaptor<Iterable<PostPopularityStat>> captor =
                ArgumentCaptor.forClass(Iterable.class);

        postViewService.refreshPopularityStats();

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
    @DisplayName("5분 갱신 시 새 버킷을 더하고 30분과 60분에서 만료된 버킷을 차감한다")
    void incrementalRefreshAddsAndExpiresBuckets() {
        when(clock.instant()).thenReturn(NOW);
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
        when(postViewBucketRepository.findByBucketStartAtIn(any()))
                .thenReturn(relevantBuckets);
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount())
                .thenReturn(List.of(1L));
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of(stat));

        postViewService.refreshPopularityStats();

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
        when(clock.instant()).thenReturn(NOW);
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
        when(postViewBucketRepository.findByBucketStartAtIn(any()))
                .thenReturn(List.of());
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount())
                .thenReturn(List.of(1L));
        when(postPopularityStatRepository.findAllById(any()))
                .thenReturn(List.of(stat));

        postViewService.refreshPopularityStats();

        assertThat(stat.getViewCount5m()).isZero();
        assertThat(stat.getViewCount30m()).isEqualTo(20L);
        assertThat(stat.getViewCount60m()).isEqualTo(40L);
        assertThat(stat.getPopularityScore()).isEqualTo(60L);
    }

    @Test
    @DisplayName("변경할 인기 통계가 없어도 전체 집계 체크포인트를 전진시킨다")
    void incrementalRefreshAdvancesCheckpointWithoutAffectedStats() {
        when(clock.instant()).thenReturn(NOW);
        PopularityAggregationCheckpoint checkpoint =
                new PopularityAggregationCheckpoint(
                        PopularityAggregationCheckpoint.JOB_NAME
                );
        checkpoint.advanceTo(Instant.parse("2026-08-02T13:55:00Z"));
        when(checkpointRepository.findByJobNameForUpdate(
                PopularityAggregationCheckpoint.JOB_NAME
        )).thenReturn(Optional.of(checkpoint));
        when(postViewBucketRepository.findByBucketStartAtIn(any()))
                .thenReturn(List.of());
        when(postPopularityStatRepository
                .findPostNumsWithNonZeroFiveMinuteCount())
                .thenReturn(List.of());

        postViewService.refreshPopularityStats();

        assertThat(checkpoint.getLastProcessedEndAt())
                .isEqualTo(COMPLETED_WINDOW_END);
    }

    @Test
    @DisplayName("인기 점수 기준 상위 10개 게시글 번호를 조회한다")
    void getTop10PopularPostNumsUsesTenItemPage() {
        when(postPopularityStatRepository.findPopularPostNums(any(Pageable.class)))
                .thenReturn(List.of(3L, 2L, 1L));

        List<Long> result = postViewService.getTop10PopularPostNums();

        assertThat(result).containsExactly(3L, 2L, 1L);
        verify(postPopularityStatRepository).findPopularPostNums(
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                )
        );
    }

    private PostViewBucket bucket(long postNum, String bucketStartAt, long count) {
        return new PostViewBucket(
                post(postNum),
                Instant.parse(bucketStartAt),
                count
        );
    }

    private Post post(long postNum) {
        Post post = mock(Post.class);
        when(post.getPostNum()).thenReturn(postNum);
        return post;
    }

    private PostPopularityStat persistedPopularityStat(long postNum) {
        PostPopularityStat stat = new PostPopularityStat(mock(Post.class));
        ReflectionTestUtils.setField(stat, "postNum", postNum);
        return stat;
    }
}
