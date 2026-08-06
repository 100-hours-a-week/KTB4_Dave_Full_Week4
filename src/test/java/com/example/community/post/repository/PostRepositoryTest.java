package com.example.community.post.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostRepositoryTest {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private SignInfoRepository signInfoRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;

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
    @DisplayName("게시글 번호 목록으로 게시글 조회")
    class FindPostByPostNumIn {

        @Test
        @DisplayName("해당하는 게시글이 없으면 빈 목록을 반환한다")
        void returnsEmptyListWhenPostsDoNotExist() {
            assertThat(postRepository.findPostByPostNumIn(
                    List.of(Long.MAX_VALUE)
            )).isEmpty();
        }

        @Test
        @DisplayName("빈 게시글 번호 목록을 전달하면 빈 목록을 반환한다")
        void returnsEmptyListWhenPostNumsAreEmpty() {
            assertThat(postRepository.findPostByPostNumIn(List.of()))
                    .isEmpty();
        }

        @Test
        @DisplayName("번호 목록에 해당하는 게시글만 반환한다")
        void returnsOnlyPostsMatchingPostNums() {
            Post firstPost = savePost(author, "first");
            Post secondPost = savePost(author, "second");
            savePost(author, "not-requested");

            List<Post> result = postRepository.findPostByPostNumIn(
                    List.of(firstPost.getPostNum(), secondPost.getPostNum())
            );

            assertThat(result)
                    .extracting(Post::getPostNum)
                    .containsExactlyInAnyOrder(
                            firstPost.getPostNum(),
                            secondPost.getPostNum()
                    );
        }

        @Test
        @DisplayName("삭제된 게시글은 번호 목록에 있어도 제외한다")
        void excludesDeletedPosts() {
            Post visiblePost = savePost(author, "visible");
            Post deletedPost = savePost(author, "deleted");
            deletedPost.delete();
            postRepository.flush();

            List<Post> result = postRepository.findPostByPostNumIn(
                    List.of(visiblePost.getPostNum(), deletedPost.getPostNum())
            );

            assertThat(result)
                    .extracting(Post::getPostNum)
                    .containsExactly(visiblePost.getPostNum());
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
