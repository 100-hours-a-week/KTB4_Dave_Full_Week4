package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularityViewRecorderTest {
    private static final Instant NOW = Instant.parse("2026-08-02T14:03:20Z");
    private static final Instant COMPLETED_WINDOW_END =
            Instant.parse("2026-08-02T14:00:00Z");
    private static final Duration CANDIDATE_MAX_AGE = Duration.ofHours(72);
    private static final Instant CANDIDATE_SINCE =
            NOW.minus(CANDIDATE_MAX_AGE);

    @Mock
    private PostViewBucketRepository postViewBucketRepository;

    @Mock
    private Clock clock;

    @Mock
    private PopularPostProperties popularPostProperties;

    private PopularityViewRecorder viewRecorder;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(popularPostProperties.candidateMaxAge())
                .thenReturn(CANDIDATE_MAX_AGE);
        PopularityWindowPolicy windowPolicy = new PopularityWindowPolicy(
                popularPostProperties
        );
        viewRecorder = new PopularityViewRecorder(
                postViewBucketRepository,
                clock,
                windowPolicy
        );
    }

    @Test
    @DisplayName("정확히 72시간 된 게시글 조회수를 현재 5분 버킷에 기록한다")
    void recordViewIncludesExactCandidateBoundary() {
        viewRecorder.recordView(1L, CANDIDATE_SINCE);

        verify(postViewBucketRepository).upsertViewCount(
                1L,
                COMPLETED_WINDOW_END,
                1L,
                CANDIDATE_SINCE
        );
    }

    @Test
    @DisplayName("72시간을 초과한 게시글 조회수는 인기글 버킷에 기록하지 않는다")
    void recordViewExcludesPostOlderThanCandidateBoundary() {
        viewRecorder.recordView(1L, CANDIDATE_SINCE.minusSeconds(1));

        verifyNoInteractions(postViewBucketRepository);
    }












}
