package com.example.community.user.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.repository.PostRepository;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserLikePost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserLikeRepositoryTest {

    @Autowired
    private UserLikeRepository userLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private UserInfo liker;
    private UserInfo author;

    @BeforeEach
    void setUp() {
        liker = saveUser("liker@example.com", "liker");
        author = saveUser("author@example.com", "author");
    }

    @Test
    @DisplayName("좋아요 수 역순으로 정렬하고 동점이면 게시글 번호 역순으로 정렬한다")
    void findByProfileIdSortsByLikeCountThenPostNumDescending() {
        Post lowerPostNum = saveLikedPost("first", 5, 0);
        Post higherPostNum = saveLikedPost("second", 5, 0);
        Post fewerLikes = saveLikedPost("third", 2, 0);
        userLikeRepository.flush();

        Page<UserLikePost> result = userLikeRepository.findByUserInfo_ProfileId(
                liker.getProfileId(),
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "post.postState.likeCount")
                                .and(Sort.by(Sort.Direction.DESC, "post.postNum"))
                )
        );

        assertThat(result.getContent())
                .extracting(userLikePost -> userLikePost.getPost().getPostNum())
                .containsExactly(
                        higherPostNum.getPostNum(),
                        lowerPostNum.getPostNum(),
                        fewerLikes.getPostNum()
                );
    }

    @Test
    @DisplayName("조회수 역순으로 정렬하고 동점이면 게시글 번호 역순으로 정렬한다")
    void findByProfileIdSortsByViewCountThenPostNumDescending() {
        Post lowerPostNum = saveLikedPost("first", 1, 5);
        Post higherPostNum = saveLikedPost("second", 1, 5);
        Post fewerViews = saveLikedPost("third", 1, 2);
        userLikeRepository.flush();

        Page<UserLikePost> result = userLikeRepository.findByUserInfo_ProfileId(
                liker.getProfileId(),
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "post.postState.viewCount")
                                .and(Sort.by(Sort.Direction.DESC, "post.postNum"))
                )
        );

        assertThat(result.getContent())
                .extracting(userLikePost -> userLikePost.getPost().getPostNum())
                .containsExactly(
                        higherPostNum.getPostNum(),
                        lowerPostNum.getPostNum(),
                        fewerViews.getPostNum()
                );
    }

    @Test
    @DisplayName("첫 페이지의 최소 크기에서 가장 최근 게시글을 반환한다")
    void findByProfileIdReturnsLatestPostAtFirstPageBoundary() {
        saveLikedPost("first", 1, 0);
        saveLikedPost("second", 1, 0);
        Post latestPost = saveLikedPost("third", 1, 0);
        userLikeRepository.flush();

        Page<UserLikePost> result = userLikeRepository.findByUserInfo_ProfileId(
                liker.getProfileId(),
                PageRequest.of(
                        0,
                        1,
                        Sort.by(Sort.Direction.DESC, "post.postNum")
                )
        );

        assertThat(result.getContent())
                .singleElement()
                .extracting(userLikePost -> userLikePost.getPost().getPostNum())
                .isEqualTo(latestPost.getPostNum());
        assertThat(result.getNumber()).isZero();
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("삭제된 게시글은 좋아요한 게시글 조회 결과에 포함하지 않는다")
    void findByProfileIdExcludesDeletedPosts() {
        Post visiblePost = saveLikedPost("visible", 1, 0);
        Post deletedPost = saveLikedPost("deleted", 1, 0);
        deletedPost.delete();
        userLikeRepository.flush();

        Page<UserLikePost> result = findLatestLikedPosts();

        assertThat(result.getContent())
                .extracting(userLikePost -> userLikePost.getPost().getPostNum())
                .containsExactly(visiblePost.getPostNum());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("신고 5회 게시글은 노출하고 블라인드된 게시글은 제외한다")
    void findByProfileIdExcludesBlindPostsAtReportCountBoundary() {
        Post reportCountBoundaryPost = saveLikedPost("report-count-five", 1, 0);
        report(reportCountBoundaryPost, 5);
        Post blindPost = saveLikedPost("report-count-six", 1, 0);
        report(blindPost, 6);
        userLikeRepository.flush();

        Page<UserLikePost> result = findLatestLikedPosts();

        assertThat(result.getContent())
                .extracting(userLikePost -> userLikePost.getPost().getPostNum())
                .containsExactly(reportCountBoundaryPost.getPostNum());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("프로필과 게시글에 해당하는 좋아요가 존재하는지 확인한다")
    void existsByProfileIdAndPostNumReturnsWhetherLikeExists() {
        Post likedPost = saveLikedPost("liked", 1, 0);
        userLikeRepository.flush();

        assertThat(userLikeRepository.existsByUserInfo_ProfileIdAndPost_PostNum(
                liker.getProfileId(),
                likedPost.getPostNum()
        )).isTrue();
        assertThat(userLikeRepository.existsByUserInfo_ProfileIdAndPost_PostNum(
                author.getProfileId(),
                likedPost.getPostNum()
        )).isFalse();
    }

    @Test
    @DisplayName("프로필과 게시글에 해당하는 좋아요를 조회한다")
    void findByProfileIdAndPostNumReturnsLike() {
        Post likedPost = saveLikedPost("liked", 1, 0);
        userLikeRepository.flush();

        assertThat(userLikeRepository.findByUserInfo_ProfileIdAndPost_PostNum(
                liker.getProfileId(),
                likedPost.getPostNum()
        )).hasValueSatisfying(userLikePost -> {
            assertThat(userLikePost.getUserInfo().getProfileId())
                    .isEqualTo(liker.getProfileId());
            assertThat(userLikePost.getPost().getPostNum())
                    .isEqualTo(likedPost.getPostNum());
        });
    }

    @Test
    @DisplayName("프로필과 게시글에 해당하는 좋아요가 없으면 빈 값을 반환한다")
    void findByProfileIdAndPostNumReturnsEmptyWhenLikeDoesNotExist() {
        Post likedPost = saveLikedPost("liked", 1, 0);
        userLikeRepository.flush();

        assertThat(userLikeRepository.findByUserInfo_ProfileIdAndPost_PostNum(
                author.getProfileId(),
                likedPost.getPostNum()
        )).isEmpty();
    }

    private UserInfo saveUser(String email, String nickname) {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo(email, "encoded-password")
        );
        return userInfoRepository.save(
                new UserInfo(signInfo, nickname, null)
        );
    }

    private Post saveLikedPost(
            String title,
            int likeCount,
            int viewCount
    ) {
        Post post = postRepository.save(
                new Post(author, title, "content", null)
        );
        userLikeRepository.save(new UserLikePost(liker, post));

        for (int currentLikeCount = 1; currentLikeCount < likeCount; currentLikeCount++) {
            UserInfo additionalLiker = saveUser(
                    title + "-liker-" + currentLikeCount + "@example.com",
                    title + "-liker-" + currentLikeCount
            );
            userLikeRepository.save(new UserLikePost(additionalLiker, post));
        }
        for (int currentViewCount = 0; currentViewCount < viewCount; currentViewCount++) {
            post.view();
        }
        return post;
    }

    private Page<UserLikePost> findLatestLikedPosts() {
        return userLikeRepository.findByUserInfo_ProfileId(
                liker.getProfileId(),
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "post.postNum")
                )
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
