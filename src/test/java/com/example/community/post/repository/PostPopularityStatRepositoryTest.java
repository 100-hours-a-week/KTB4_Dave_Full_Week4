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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostPopularityStatRepositoryTest {
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
    @DisplayName("동점이면 30분 조회수가 높은 글을 우선하고 유효하지 않은 글은 제외한다")
    void findPopularPostNumsPrioritizesRecentViewsAndFiltersIneligiblePosts() {
        Post recentPost = savePost("recent");
        Post olderPost = savePost("older");
        Post deletedPost = savePost("deleted");
        Post blindPost = savePost("blind");

        deletedPost.delete();
        for (int count = 0; count < 6; count++) {
            blindPost.report();
        }

        postPopularityStatRepository.saveAll(List.of(
                popularityStat(recentPost, 5L, 15L, 20L),
                popularityStat(olderPost, 5L, 10L, 25L),
                popularityStat(deletedPost, 100L, 100L, 100L),
                popularityStat(blindPost, 100L, 100L, 100L)
        ));
        postPopularityStatRepository.flush();

        List<Long> result = postPopularityStatRepository.findPopularPostNums(
                PageRequest.of(0, 10)
        );
        List<Long> limitedResult =
                postPopularityStatRepository.findPopularPostNums(
                        PageRequest.of(0, 1)
                );

        assertThat(result).containsExactly(
                recentPost.getPostNum(),
                olderPost.getPostNum()
        );
        assertThat(limitedResult).containsExactly(recentPost.getPostNum());
    }

    private Post savePost(String title) {
        return postRepository.saveAndFlush(
                new Post(author, title, "content", null)
        );
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
}
