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
    @DisplayName("요청한 프로필이 좋아요한 게시글만 반환한다")
    void findByProfileIdReturnsOnlyRequestedUsersLikes() {
        Post likedPost = saveLikedPost("liked");
        Post otherUsersLikedPost = postRepository.save(
                new Post(author, "other", "content", null)
        );
        userLikeRepository.save(new UserLikePost(author, otherUsersLikedPost));
        userLikeRepository.flush();

        Page<UserLikePost> result = findLatestLikedPosts();

        assertThat(result.getContent())
                .extracting(userLikePost -> userLikePost.getPost().getPostNum())
                .containsExactly(likedPost.getPostNum());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("삭제된 게시글은 좋아요한 게시글 조회 결과에 포함하지 않는다")
    void findByProfileIdExcludesDeletedPosts() {
        Post visiblePost = saveLikedPost("visible");
        Post deletedPost = saveLikedPost("deleted");
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
        Post reportCountBoundaryPost = saveLikedPost("report-count-five");
        report(reportCountBoundaryPost, 5);
        Post blindPost = saveLikedPost("report-count-six");
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
        Post likedPost = saveLikedPost("liked");
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
        Post likedPost = saveLikedPost("liked");
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
        Post likedPost = saveLikedPost("liked");
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

    private Post saveLikedPost(String title) {
        Post post = postRepository.save(
                new Post(author, title, "content", null)
        );
        userLikeRepository.save(new UserLikePost(liker, post));
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
