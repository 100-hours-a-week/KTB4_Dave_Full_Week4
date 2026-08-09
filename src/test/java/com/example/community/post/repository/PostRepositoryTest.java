package com.example.community.post.repository;

import com.example.community.post.dto.query.PostStateData;
import com.example.community.post.dto.response.PopularPostTitleResponse;
import com.example.community.post.entity.Post;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MICROS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
class PostRepositoryTest {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private SignInfoRepository signInfoRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private PostStateRepository postStateRepository;

    private UserInfo author;

    @BeforeEach
    void setUp() {
        author = saveUser("test@gmail.com", "test");
    }

    @Nested
    @DisplayName("전체 게시글 목록 조회")
    class FindPostByPage {

        @Test
        @DisplayName("게시글이 없으면 빈 페이지를 반환한다")
        void returnsEmptyPageWhenPostDoesNotExist() {
            Page<Post> result = postRepository.findPostByPage(
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("게시글이 있으면 페이지로 반환한다")
        void returnsPageWhenPostExists() {
            Post post = savePost(author, "title");

            Page<Post> result = postRepository.findPostByPage(
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(Post::getPostNum)
                    .containsExactly(post.getPostNum());
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("삭제된 게시글은 전체 목록에서 제외한다")
        void excludesDeletedPosts() {
            Post visiblePost = savePost(author, "visible");
            Post deletedPost = savePost(author, "deleted");
            deletedPost.delete();
            postRepository.flush();

            Page<Post> result = postRepository.findPostByPage(
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(Post::getPostNum)
                    .containsExactly(visiblePost.getPostNum());
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("게시글 번호로 게시글 조회")
    class FindByPostNum {

        @Test
        @DisplayName("게시글이 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenPostDoesNotExist() {
            assertThat(postRepository.findByPostNum(Long.MAX_VALUE))
                    .isEmpty();
        }

        @Test
        @DisplayName("게시글이 있으면 해당 게시글을 반환한다")
        void returnsPostWhenPostExists() {
            Post post = savePost(author, "title");

            assertThat(postRepository.findByPostNum(post.getPostNum()))
                    .hasValueSatisfying(foundPost ->
                            assertThat(foundPost.getPostNum())
                                    .isEqualTo(post.getPostNum())
                    );
        }

        @Test
        @DisplayName("삭제된 게시글은 번호로 조회해도 반환하지 않는다")
        void doesNotReturnDeletedPost() {
            Post deletedPost = savePost(author, "deleted");
            deletedPost.delete();
            postRepository.flush();

            assertThat(postRepository.findByPostNum(deletedPost.getPostNum()))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("인기글 안정 요약 조회")
    class FindPopularPostSummariesByPostNumIn {

        @Test
        @DisplayName("상태 엔티티 없이 요청한 게시글의 안정 필드를 반환한다")
        void returnsStableSummaryFields() {
            Post post = savePost(author, "popular");

            List<PopularPostTitleResponse> result =
                    postRepository.findPopularPostTitlesByPostNumIn(
                            List.of(post.getPostNum())
                    );

            assertThat(result).singleElement().satisfies(summary -> {
                assertThat(summary.postNum()).isEqualTo(post.getPostNum());
                assertThat(summary.nickname()).isEqualTo("test");
                assertThat(summary.title()).isEqualTo("popular");
                assertThat(summary.writeAt().toInstant())
                        .isCloseTo(post.getWriteAt(), within(1, MICROS));
            });
        }

        @Test
        @DisplayName("삭제된 게시글은 안정 요약에서 제외한다")
        void excludesDeletedPost() {
            Post post = savePost(author, "deleted");
            post.delete();
            postRepository.flush();

            assertThat(postRepository.findPopularPostTitlesByPostNumIn(
                    List.of(post.getPostNum())
            )).isEmpty();
        }
    }

    @Nested
    @DisplayName("게시글 상세 분리 projection 조회")
    class FindPostDetailProjections {

        @Test
        @DisplayName("본문 projection은 상태 엔티티 없이 본문 필드만 반환한다")
        void returnsBodyFields() {
            Post post = savePost(author, "detail-title");

            assertThat(postRepository.findPostBodyDataByPostNum(post.getPostNum()))
                    .hasValueSatisfying(body -> {
                        assertThat(body.postNum()).isEqualTo(post.getPostNum());
                        assertThat(body.title()).isEqualTo("detail-title");
                        assertThat(body.content()).isEqualTo("content");
                        assertThat(body.writeAt())
                                .isCloseTo(post.getWriteAt(), within(1, MICROS));
                    });
        }

        @Test
        @DisplayName("상태 projection은 상태 필드만 반환한다")
        void returnsStateFields() {
            Post post = savePost(author, "state");

            Optional<PostStateData> result =
                    postStateRepository.findDataByPostNum(
                            post.getPostNum()
                    );

            assertThat(result).hasValueSatisfying(state -> {
                assertThat(state.viewCount()).isZero();
                assertThat(state.likeCount()).isZero();
                assertThat(state.reportCount()).isZero();
                assertThat(state.commentCount()).isZero();
            });
        }

        @Test
        @DisplayName("조회수는 Post 전체 로드 없이 상태 테이블에서 증가시킨다")
        void incrementsViewCountDirectly() {
            Post post = savePost(author, "view");

            int updated = postStateRepository.incrementViewCount(
                    post.getPostNum()
            );

            assertThat(updated).isOne();
            assertThat(postStateRepository.findDataByPostNum(
                    post.getPostNum()
            )).hasValueSatisfying(state ->
                    assertThat(state.viewCount()).isOne()
            );
        }

        @Test
        @DisplayName("삭제된 게시글은 본문과 상태 projection에서 제외한다")
        void excludesDeletedPost() {
            Post post = savePost(author, "deleted-detail");
            post.delete();
            postRepository.flush();

            assertThat(postRepository.findPostBodyDataByPostNum(post.getPostNum()))
                    .isEmpty();
            assertThat(postStateRepository.findDataByPostNum(
                    post.getPostNum()
            )).isEmpty();
        }
    }

    @Nested
    @DisplayName("특정 사용자가 작성한 게시글 목록 조회")
    class FindPostByUserInfoProfileId {

        @Test
        @DisplayName("사용자가 작성한 게시글이 없으면 빈 페이지를 반환한다")
        void returnsEmptyPageWhenUserHasNoPosts() {
            Page<Post> result = postRepository.findPostByUserInfo_ProfileId(
                    author.getProfileId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("해당 사용자가 작성한 게시글만 반환한다")
        void returnsOnlyPostsWrittenByUser() {
            Post firstPost = savePost(author, "first");
            Post secondPost = savePost(author, "second");
            UserInfo otherAuthor = saveUser("other@gmail.com", "other");
            savePost(otherAuthor, "other-post");

            Page<Post> result = postRepository.findPostByUserInfo_ProfileId(
                    author.getProfileId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(Post::getPostNum)
                    .containsExactlyInAnyOrder(
                            firstPost.getPostNum(),
                            secondPost.getPostNum()
                    );
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("삭제된 게시글은 사용자의 게시글 목록에서 제외한다")
        void excludesDeletedPosts() {
            Post visiblePost = savePost(author, "visible");
            Post deletedPost = savePost(author, "deleted");
            deletedPost.delete();
            postRepository.flush();

            Page<Post> result = postRepository.findPostByUserInfo_ProfileId(
                    author.getProfileId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(Post::getPostNum)
                    .containsExactly(visiblePost.getPostNum());
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    private UserInfo saveUser(String email, String nickname) {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo(email, "encodedPassword")
        );
        return userInfoRepository.save(
                new UserInfo(signInfo, nickname, null)
        );
    }

    private Post savePost(UserInfo userInfo, String title) {
        return postRepository.save(
                new Post(userInfo, title, "content", null)
        );
    }
}
