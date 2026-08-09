package com.example.community.post.service;

import com.example.community.post.configuration.PopularPostProperties;
import com.example.community.post.entity.PopularityAggregationCheckpoint;
import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import com.example.community.post.repository.PopularityAggregationCheckpointRepository;
import com.example.community.post.repository.PostPopularityStatRepository;
import com.example.community.post.repository.PostRepository;
import com.example.community.post.repository.PostViewBucketRepository;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@Import({
        PopularityCleanupService.class,
        PopularityCheckpointLock.class,
        PopularityWindowPolicy.class,
        PostViewCleanupTransactionTest.FixedTimeConfiguration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostViewCleanupTransactionTest {
    private static final Instant NOW =
            Instant.parse("2026-08-04T12:00:00Z");
    private static final Instant CANDIDATE_SINCE =
            NOW.minus(Duration.ofHours(72));
    private static final Instant BUCKET_START_AT =
            Instant.parse("2026-08-04T11:55:00Z");

    @Autowired
    private PopularityCleanupService cleanupService;

    @Autowired
    private PostPopularityStatRepository postPopularityStatRepository;

    @MockitoSpyBean
    private PostViewBucketRepository postViewBucketRepository;

    @Autowired
    private PopularityAggregationCheckpointRepository checkpointRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("버킷 삭제가 실패하면 앞서 삭제한 인기 통계도 롤백한다")
    void rollsBackStatDeletionWhenBucketDeletionFails() {
        Long expiredPostNum = saveExpiredPopularityData();
        doThrow(new DataAccessResourceFailureException("cleanup failed"))
                .when(postViewBucketRepository)
                .deleteAllByPostWriteAtBefore(CANDIDATE_SINCE);

        assertThatThrownBy(
                () -> cleanupService.cleanupExpiredPopularityData()
        ).isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessage("cleanup failed");

        assertThat(postPopularityStatRepository.existsById(expiredPostNum))
                .isTrue();
        assertThat(postViewBucketRepository.findAll())
                .extracting(bucket -> bucket.getPost().getPostNum())
                .containsExactly(expiredPostNum);
    }

    private Long saveExpiredPopularityData() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(
                transactionManager
        );
        return transactionTemplate.execute(status -> {
            checkpointRepository.save(
                    new PopularityAggregationCheckpoint(
                            PopularityAggregationCheckpoint.JOB_NAME
                    )
            );
            SignInfo signInfo = signInfoRepository.save(
                    new SignInfo("rollback@example.com", "password")
            );
            UserInfo userInfo = userInfoRepository.save(
                    new UserInfo(signInfo, "rollback-author", null)
            );
            Post post = new Post(
                    userInfo,
                    "expired",
                    "content",
                    null
            );
            ReflectionTestUtils.setField(
                    post,
                    "writeAt",
                    CANDIDATE_SINCE.minusSeconds(1)
            );
            postRepository.save(post);

            PostPopularityStat stat = new PostPopularityStat(post);
            stat.initializeCounts(1L, 1L, 1L);
            postPopularityStatRepository.save(stat);
            postViewBucketRepository.save(
                    new PostViewBucket(post, BUCKET_START_AT, 1L)
            );
            return post.getPostNum();
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTimeConfiguration {
        @Bean
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        PopularPostProperties popularPostProperties() {
            return new PopularPostProperties(Duration.ofHours(72));
        }
    }
}
