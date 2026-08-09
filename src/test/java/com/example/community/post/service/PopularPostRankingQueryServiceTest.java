package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
import com.example.community.post.repository.PostPopularityStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularPostRankingQueryServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T14:03:20Z");
    private static final Instant COMPLETED_WINDOW_END =
            Instant.parse("2026-08-02T14:00:00Z");
    private static final Duration CANDIDATE_MAX_AGE = Duration.ofHours(72);
    private static final Instant CANDIDATE_SINCE =
            NOW.minus(CANDIDATE_MAX_AGE);

    @Mock
    private PostPopularityStatRepository postPopularityStatRepository;

    @Mock
    private Clock clock;

    @Mock
    private PopularPostProperties popularPostProperties;

    private PopularPostRankingQueryService rankingQueryService;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(popularPostProperties.candidateMaxAge())
                .thenReturn(CANDIDATE_MAX_AGE);
        PopularityWindowPolicy windowPolicy = new PopularityWindowPolicy(
                popularPostProperties
        );
        rankingQueryService = new PopularPostRankingQueryService(
                postPopularityStatRepository,
                clock,
                windowPolicy
        );
    }











    @Test
    @DisplayName("인기 점수 기준 상위 10개 게시글 번호를 조회한다")
    void getTop10PopularPostNumsUsesTenItemPage() {
        when(postPopularityStatRepository.findPopularPostNums(
                any(Instant.class),
                any(Pageable.class)
        ))
                .thenReturn(List.of(3L, 2L, 1L));

        List<Long> result = rankingQueryService.getTop10PopularPostNums();

        assertThat(result).containsExactly(3L, 2L, 1L);
        verify(postPopularityStatRepository).findPopularPostNums(
                eq(CANDIDATE_SINCE),
                org.mockito.ArgumentMatchers.argThat(pageable ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 10
                )
        );
    }



}
