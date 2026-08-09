package com.example.community.post.fixture;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostPopularityStat;
import com.example.community.post.entity.PostViewBucket;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PopularityTestFixture {
    private PopularityTestFixture() {
    }

    public static PostViewBucket bucket(
            long postNum,
            String bucketStartAt,
            long count
    ) {
        return new PostViewBucket(
                post(postNum),
                Instant.parse(bucketStartAt),
                count
        );
    }

    public static Post post(long postNum) {
        Post post = mock(Post.class);
        when(post.getPostNum()).thenReturn(postNum);
        return post;
    }

    public static PostPopularityStat persistedPopularityStat(long postNum) {
        PostPopularityStat stat = new PostPopularityStat(mock(Post.class));
        ReflectionTestUtils.setField(stat, "postNum", postNum);
        return stat;
    }
}
