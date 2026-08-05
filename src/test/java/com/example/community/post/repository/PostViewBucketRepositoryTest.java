package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=" +
                "jdbc:h2:mem:post-view-bucket;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostViewBucketRepositoryTest {
    private static final Instant FIXED_NOW =
            Instant.parse("2026-08-04T12:00:00Z");
    private static final Instant CANDIDATE_SINCE =
            FIXED_NOW.minusSeconds(72 * 60 * 60);
    private static final Instant BUCKET_START_AT =
            Instant.parse("2026-08-04T11:55:00Z");

    @Autowired
    private PostViewBucketRepository postViewBucketRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private EntityManager entityManager;

    private UserInfo author;

    @BeforeEach
    void setUp() {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo("bucket@example.com", "password")
        );
        author = userInfoRepository.save(
                new UserInfo(signInfo, "bucket-author", null)
        );
    }

    @Test
    @DisplayName("native upsert는 DB 작성 시각을 다시 검사해 오래된 게시글 버킷을 생성하지 않는다")
    void upsertDefendsCandidateBoundaryUsingPersistedWriteAt() {
        Post recentPost = savePost(
                "recent",
                FIXED_NOW.minusSeconds(60)
        );
        Post boundaryPost = savePost("boundary", CANDIDATE_SINCE);
        Post expiredPost = savePost(
                "expired",
                CANDIDATE_SINCE.minusSeconds(1)
        );

        int boundaryInsertedCount = postViewBucketRepository.upsertViewCount(
                boundaryPost.getPostNum(),
                BUCKET_START_AT,
                1L,
                CANDIDATE_SINCE
        );
        int boundaryUpdatedCount = postViewBucketRepository.upsertViewCount(
                boundaryPost.getPostNum(),
                BUCKET_START_AT,
                1L,
                CANDIDATE_SINCE
        );
        int recentInsertedCount = postViewBucketRepository.upsertViewCount(
                recentPost.getPostNum(),
                BUCKET_START_AT,
                1L,
                CANDIDATE_SINCE
        );
        int expiredCount = postViewBucketRepository.upsertViewCount(
                expiredPost.getPostNum(),
                BUCKET_START_AT,
                1L,
                CANDIDATE_SINCE
        );
        entityManager.clear();

        assertThat(boundaryInsertedCount).isEqualTo(1);
        assertThat(boundaryUpdatedCount).isEqualTo(2);
        assertThat(recentInsertedCount).isEqualTo(1);
        assertThat(expiredCount).isZero();
        assertThat(postViewBucketRepository.findAll())
                .extracting(
                        bucket -> bucket.getPost().getPostNum(),
                        PostViewBucket::getViewCount
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                recentPost.getPostNum(),
                                1L
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                boundaryPost.getPostNum(),
                                2L
                        )
                );
    }

    @Test
    @DisplayName("최초 집계와 증분 집계는 72시간을 초과한 게시글 버킷을 제외한다")
    void aggregationQueriesExcludeExpiredPostBuckets() {
        Post recentPost = savePost(
                "recent",
                FIXED_NOW.minusSeconds(60)
        );
        Post boundaryPost = savePost("boundary", CANDIDATE_SINCE);
        Post expiredPost = savePost(
                "expired",
                CANDIDATE_SINCE.minusSeconds(1)
        );
        saveBuckets(
                new PostViewBucket(recentPost, BUCKET_START_AT, 1L),
                new PostViewBucket(boundaryPost, BUCKET_START_AT, 1L),
                new PostViewBucket(expiredPost, BUCKET_START_AT, 100L)
        );

        List<PostViewBucket> rebuildBuckets = postViewBucketRepository
                .findForPopularityRebuild(
                        FIXED_NOW.minusSeconds(60 * 60),
                        FIXED_NOW,
                        CANDIDATE_SINCE
                );
        List<PostViewBucket> rollingBuckets = postViewBucketRepository
                .findForRollingWindow(
                        List.of(BUCKET_START_AT),
                        CANDIDATE_SINCE
                );

        assertThat(rebuildBuckets)
                .extracting(bucket -> bucket.getPost().getPostNum())
                .containsExactlyInAnyOrder(
                        recentPost.getPostNum(),
                        boundaryPost.getPostNum()
                );
        assertThat(rollingBuckets)
                .extracting(bucket -> bucket.getPost().getPostNum())
                .containsExactlyInAnyOrder(
                        recentPost.getPostNum(),
                        boundaryPost.getPostNum()
                );
    }

    @Test
    @DisplayName("일일 정리는 72시간을 초과한 게시글 버킷만 삭제한다")
    void deletesOnlyExpiredPostBuckets() {
        Post recentPost = savePost(
                "recent",
                FIXED_NOW.minusSeconds(60)
        );
        Post boundaryPost = savePost("boundary", CANDIDATE_SINCE);
        Post expiredPost = savePost(
                "expired",
                CANDIDATE_SINCE.minusSeconds(1)
        );
        saveBuckets(
                new PostViewBucket(recentPost, BUCKET_START_AT, 1L),
                new PostViewBucket(boundaryPost, BUCKET_START_AT, 1L),
                new PostViewBucket(expiredPost, BUCKET_START_AT, 1L)
        );

        int deletedCount = postViewBucketRepository
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE);
        postViewBucketRepository.flush();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(postViewBucketRepository.findAll())
                .extracting(bucket -> bucket.getPost().getPostNum())
                .containsExactlyInAnyOrder(
                        recentPost.getPostNum(),
                        boundaryPost.getPostNum()
                );
    }

    private Post savePost(String title, Instant writeAt) {
        Post post = new Post(author, title, "content", null);
        ReflectionTestUtils.setField(post, "writeAt", writeAt);
        return postRepository.saveAndFlush(post);
    }

    private void saveBuckets(PostViewBucket... buckets) {
        postViewBucketRepository.saveAll(List.of(buckets));
        postViewBucketRepository.flush();
    }
}
