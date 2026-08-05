package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostEditRepositoryTest {

    @Autowired
    private PostEditRepository postEditRepository;

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
                new SignInfo("author@example.com", "encoded-password")
        );
        author = userInfoRepository.save(
                new UserInfo(signInfo, "author", null)
        );
    }

    @Nested
    @DisplayName("수정 이력 번호로 조회")
    class FindById {

        @Test
        @DisplayName("수정 이력이 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenEditRecordDoesNotExist() {
            assertThat(postEditRepository.findById(Long.MAX_VALUE))
                    .isEmpty();
        }

        @Test
        @DisplayName("수정 이력이 있으면 해당 이력을 반환한다")
        void returnsEditRecordWhenItExists() {
            Post post = savePost("post");
            PostEditRecord editRecord = saveEditRecord(post, 0);

            assertThat(postEditRepository.findById(editRecord.getEditId()))
                    .hasValueSatisfying(foundRecord ->
                            assertThat(foundRecord.getEditId())
                                    .isEqualTo(editRecord.getEditId())
                    );
        }
    }

    @Nested
    @DisplayName("게시글의 수정 이력 목록 조회")
    class FindByPostNum {

        @Test
        @DisplayName("게시글에 수정 이력이 없으면 빈 페이지를 반환한다")
        void returnsEmptyPageWhenPostHasNoEditRecords() {
            Post post = savePost("post");

            Page<PostEditRecord> result = findEditRecords(post.getPostNum());

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("게시글이 없으면 빈 페이지를 반환한다")
        void returnsEmptyPageWhenPostDoesNotExist() {
            Page<PostEditRecord> result = findEditRecords(Long.MAX_VALUE);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("해당 게시글의 수정 이력을 최신 이력 순으로 반환한다")
        void returnsOnlyPostEditRecordsInDescendingOrder() {
            Post post = savePost("post");
            PostEditRecord olderRecord = saveEditRecord(post, 0);
            PostEditRecord latestRecord = saveEditRecord(post, 1);
            Post otherPost = savePost("other-post");
            saveEditRecord(otherPost, 0);
            postEditRepository.flush();

            Page<PostEditRecord> result = findEditRecords(post.getPostNum());

            assertThat(result.getContent())
                    .extracting(PostEditRecord::getEditId)
                    .containsExactly(
                            latestRecord.getEditId(),
                            olderRecord.getEditId()
                    );
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }

    private Post savePost(String title) {
        return postRepository.save(
                new Post(author, title, "content", null)
        );
    }

    private PostEditRecord saveEditRecord(Post post, int version) {
        return postEditRepository.save(
                new PostEditRecord(
                        post,
                        version,
                        "title-" + version,
                        "content-" + version,
                        null,
                        Instant.now()
                )
        );
    }

    private Page<PostEditRecord> findEditRecords(long postNum) {
        return postEditRepository.findByPost_PostNumOrderByEditIdDesc(
                postNum,
                PageRequest.of(0, 10)
        );
    }
}
