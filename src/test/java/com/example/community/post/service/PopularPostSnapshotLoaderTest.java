package com.example.community.post.service;

import com.example.community.post.cache.PopularPostSnapshot;
import com.example.community.post.dto.response.PopularPostTitleResponse;
import com.example.community.post.repository.PostRepository;
import com.example.community.util.ImageUrlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopularPostSnapshotLoaderTest {
    @Mock
    private PopularPostRankingQueryService rankingQueryService;
    @Mock
    private PostRepository postRepository;
    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );
    @InjectMocks
    private PopularPostSnapshotLoader loader;

    @Test
    @DisplayName("요약 조회 결과를 인기 ID 순서로 재정렬한다")
    void preservesPopularityOrder() {
        when(rankingQueryService.getTop10PopularPostNums())
                .thenReturn(List.of(3L, 1L, 2L));
        when(postRepository.findPopularPostTitlesByPostNumIn(
                List.of(3L, 1L, 2L)
        )).thenReturn(List.of(summary(1L), summary(2L), summary(3L)));

        PopularPostSnapshot snapshot = loader.load();

        assertThat(snapshot.orderedPosts())
                .extracting(PopularPostTitleResponse::postNum)
                .containsExactly(3L, 1L, 2L);
        assertThat(snapshot.orderedPosts())
                .extracting(PopularPostTitleResponse::profileImage)
                .containsOnly(
                        "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
                                + "profiles/author.png"
                );
        assertThat(snapshot.postNums()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("조회 중 삭제되어 요약이 없는 게시글은 스냅샷에서 제외한다")
    void skipsMissingSummary() {
        when(rankingQueryService.getTop10PopularPostNums())
                .thenReturn(List.of(1L, 2L));
        when(postRepository.findPopularPostTitlesByPostNumIn(
                List.of(1L, 2L)
        )).thenReturn(List.of(summary(2L)));

        PopularPostSnapshot snapshot = loader.load();

        assertThat(snapshot.orderedPosts())
                .extracting(PopularPostTitleResponse::postNum)
                .containsExactly(2L);
        assertThat(snapshot.postNums()).containsExactly(2L);
    }

    @Test
    @DisplayName("인기 ID가 없으면 IN 요약 쿼리를 실행하지 않는다")
    void doesNotQuerySummariesWhenPopularityIsEmpty() {
        when(rankingQueryService.getTop10PopularPostNums())
                .thenReturn(List.of());

        PopularPostSnapshot snapshot = loader.load();

        assertThat(snapshot.orderedPosts()).isEmpty();
        assertThat(snapshot.postNums()).isEmpty();
        verify(postRepository, never())
                .findPopularPostTitlesByPostNumIn(List.of());
    }

    private PopularPostTitleResponse summary(long postNum) {
        return new PopularPostTitleResponse(
                postNum,
                "author",
                "profiles/author.png",
                null,
                "title-" + postNum,
                Instant.EPOCH
        );
    }
}
