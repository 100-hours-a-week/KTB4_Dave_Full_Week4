package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostPopularityStatRepositoryTest {
    private static final Instant NOW =
            Instant.parse("2026-08-04T12:00:00Z");
    private static final Instant CANDIDATE_SINCE =
            NOW.minusSeconds(72 * 60 * 60);

    @Autowired
    private PostPopularityStatRepository postPopularityStatRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private UserInfo author;

    @BeforeEach
    void setUp() {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo("popular@example.com", "password")
        );
        author = userInfoRepository.save(
                new UserInfo(signInfo, "popular-author", null)
        );
    }

    @Test
    @DisplayName("집계 값이 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenPopularityStatsDoNotExist() {
        assertThat(findPopularPostNums()).isEmpty();
    }

    @Test
    @DisplayName("인기 점수가 높은 게시글을 먼저 반환한다")
    void prioritizesHigherPopularityScore() {
        Post lowerScorePost = savePost("lower-score");
        Post higherScorePost = savePost("higher-score");
        saveStats(
                popularityStat(lowerScorePost, 1L, 1L, 5L),
                popularityStat(higherScorePost, 1L, 1L, 10L)
        );

        assertThat(findPopularPostNums()).containsExactly(
                higherScorePost.getPostNum(),
                lowerScorePost.getPostNum()
        );
    }

    @Test
    @DisplayName("인기 점수가 같으면 5분 조회수가 높은 게시글을 먼저 반환한다")
    void prioritizesHigherFiveMinuteCountWhenScoresTie() {
        Post lowerFiveMinuteCountPost = savePost("lower-five-minute-count");
        Post higherFiveMinuteCountPost = savePost("higher-five-minute-count");
        saveStats(
                popularityStat(lowerFiveMinuteCountPost, 5L, 15L, 15L),
                popularityStat(higherFiveMinuteCountPost, 10L, 10L, 10L)
        );

        assertThat(findPopularPostNums()).containsExactly(
                higherFiveMinuteCountPost.getPostNum(),
                lowerFiveMinuteCountPost.getPostNum()
        );
    }

    @Test
    @DisplayName("인기 점수와 5분 조회수가 같으면 30분 조회수가 높은 게시글을 먼저 반환한다")
    void prioritizesHigherThirtyMinuteCountWhenScoreAndFiveMinuteCountTie() {
        Post lowerThirtyMinuteCountPost = savePost("lower-thirty-minute-count");
        Post higherThirtyMinuteCountPost = savePost("higher-thirty-minute-count");
        saveStats(
                popularityStat(lowerThirtyMinuteCountPost, 5L, 15L, 25L),
                popularityStat(higherThirtyMinuteCountPost, 5L, 20L, 20L)
        );

        assertThat(findPopularPostNums()).containsExactly(
                higherThirtyMinuteCountPost.getPostNum(),
                lowerThirtyMinuteCountPost.getPostNum()
        );
    }

    @Test
    @DisplayName("모든 인기 기준이 같으면 게시글 번호 역순으로 반환한다")
    void prioritizesHigherPostNumWhenAllPopularityCriteriaTie() {
        Post lowerPostNum = savePost("lower-post-num");
        Post higherPostNum = savePost("higher-post-num");
        saveStats(
                popularityStat(lowerPostNum, 5L, 15L, 25L),
                popularityStat(higherPostNum, 5L, 15L, 25L)
        );

        assertThat(findPopularPostNums()).containsExactly(
                higherPostNum.getPostNum(),
                lowerPostNum.getPostNum()
        );
    }

    @Test
    @DisplayName("삭제되거나 블라인드된 게시글은 제외하고 신고 5회 게시글은 포함한다")
    void excludesDeletedAndBlindPosts() {
        Post visiblePost = savePost("visible");
        Post reportCountBoundaryPost = savePost("report-count-five");
        Post deletedPost = savePost("deleted");
        Post blindPost = savePost("blind");
        report(reportCountBoundaryPost, 5);
        deletedPost.delete();
        report(blindPost, 6);
        saveStats(
                popularityStat(visiblePost, 1L, 1L, 1L),
                popularityStat(reportCountBoundaryPost, 1L, 1L, 1L),
                popularityStat(deletedPost, 100L, 100L, 100L),
                popularityStat(blindPost, 100L, 100L, 100L)
        );

        assertThat(findPopularPostNums())
                .containsExactlyInAnyOrder(
                        visiblePost.getPostNum(),
                        reportCountBoundaryPost.getPostNum()
                );
    }

    @Test
    @DisplayName("5분 조회수가 0인 글과 유효하지 않은 게시글은 제외한다")
    void returnsOnlyEligiblePostNumsWithNonZeroFiveMinuteCount() {
        Post visiblePost = savePost("visible");
        Post reportCountBoundaryPost = savePost("report-count-five");
        Post zeroFiveMinuteCountPost = savePost("zero-five-minute-count");
        Post deletedPost = savePost("deleted");
        Post blindPost = savePost("blind");
        report(reportCountBoundaryPost, 5);
        deletedPost.delete();
        report(blindPost, 6);
        saveStats(
                popularityStat(visiblePost, 1L, 1L, 1L),
                popularityStat(reportCountBoundaryPost, 1L, 1L, 1L),
                popularityStat(zeroFiveMinuteCountPost, 0L, 5L, 10L),
                popularityStat(deletedPost, 100L, 100L, 100L),
                popularityStat(blindPost, 100L, 100L, 100L)
        );

        assertThat(
                postPopularityStatRepository
                        .findPostNumsWithNonZeroFiveMinuteCount(
                                CANDIDATE_SINCE
                        )
        ).containsExactlyInAnyOrder(
                visiblePost.getPostNum(),
                reportCountBoundaryPost.getPostNum()
        );
    }

    @Test
    @DisplayName("정확히 72시간 된 게시글은 포함하고 1초 더 오래된 게시글은 제외한다")
    void appliesExactSeventyTwoHourBoundaryToPopularityQueries() {
        Post boundaryPost = savePost("boundary", CANDIDATE_SINCE);
        Post expiredPost = savePost(
                "expired",
                CANDIDATE_SINCE.minusSeconds(1)
        );
        saveStats(
                popularityStat(boundaryPost, 1L, 1L, 1L),
                popularityStat(expiredPost, 100L, 100L, 100L)
        );

        assertThat(findPopularPostNums())
                .containsExactly(boundaryPost.getPostNum());
        assertThat(
                postPopularityStatRepository
                        .findPostNumsWithNonZeroFiveMinuteCount(
                                CANDIDATE_SINCE
                        )
        ).containsExactly(boundaryPost.getPostNum());
    }

    @Test
    @DisplayName("일일 정리는 72시간을 초과한 게시글의 인기 통계만 삭제한다")
    void deletesOnlyExpiredPopularityStats() {
        Post recentPost = savePost("recent", NOW.minusSeconds(60));
        Post boundaryPost = savePost("boundary", CANDIDATE_SINCE);
        Post expiredPost = savePost(
                "expired",
                CANDIDATE_SINCE.minusSeconds(1)
        );
        saveStats(
                popularityStat(recentPost, 1L, 1L, 1L),
                popularityStat(boundaryPost, 1L, 1L, 1L),
                popularityStat(expiredPost, 1L, 1L, 1L)
        );

        int deletedCount = postPopularityStatRepository
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE);
        postPopularityStatRepository.flush();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(postPopularityStatRepository.findAll())
                .extracting(PostPopularityStat::getPostNum)
                .containsExactlyInAnyOrder(
                        recentPost.getPostNum(),
                        boundaryPost.getPostNum()
                );
    }

    private Post savePost(String title) {
        return savePost(title, NOW);
    }

    private Post savePost(String title, Instant writeAt) {
        Post post = new Post(author, title, "content", null);
        ReflectionTestUtils.setField(post, "writeAt", writeAt);
        return postRepository.saveAndFlush(post);
    }

    private PostPopularityStat popularityStat(
            Post post,
            long viewCount5m,
            long viewCount30m,
            long viewCount60m
    ) {
        PostPopularityStat stat = new PostPopularityStat(post);
        stat.initializeCounts(
                viewCount5m,
                viewCount30m,
                viewCount60m
        );
        return stat;
    }

    private void saveStats(PostPopularityStat... stats) {
        postPopularityStatRepository.saveAll(List.of(stats));
        postPopularityStatRepository.flush();
    }

    private List<Long> findPopularPostNums() {
        return postPopularityStatRepository.findPopularPostNums(
                CANDIDATE_SINCE,
                PageRequest.of(0, 10)
        );
    }

    private void report(Post post, int reportCount) {
        for (int currentReportCount = 0;
             currentReportCount < reportCount;
             currentReportCount++) {
            post.report();
        }
    }
}
