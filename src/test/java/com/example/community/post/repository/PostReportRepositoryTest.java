package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostReport;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostReportRepositoryTest {

    @Autowired
    private PostReportRepository postReportRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private Post post;
    private UserInfo reporter;

    @BeforeEach
    void setUp() {
        UserInfo author = saveUser("author@example.com", "author");
        reporter = saveUser("reporter@example.com", "reporter");
        post = postRepository.save(
                new Post(author, "title", "content", null)
        );
    }

    @Test
    @DisplayName("게시글이 없으면 신고 이력이 없다")
    void returnsFalseWhenPostDoesNotExist() {
        savePostReport();

        assertThat(postReportRepository.existsByPost_PostNumAndUserInfo_ProfileId(
                Long.MAX_VALUE,
                reporter.getProfileId()
        )).isFalse();
    }

    @Test
    @DisplayName("사용자가 없으면 신고 이력이 없다")
    void returnsFalseWhenUserDoesNotExist() {
        savePostReport();

        assertThat(postReportRepository.existsByPost_PostNumAndUserInfo_ProfileId(
                post.getPostNum(),
                Long.MAX_VALUE
        )).isFalse();
    }

    @Test
    @DisplayName("게시글과 사용자가 존재해도 신고 이력이 없으면 false를 반환한다")
    void returnsFalseWhenPostReportDoesNotExist() {
        assertThat(postReportRepository.existsByPost_PostNumAndUserInfo_ProfileId(
                post.getPostNum(),
                reporter.getProfileId()
        )).isFalse();
    }

    @Test
    @DisplayName("게시글과 사용자의 신고 이력이 있으면 true를 반환한다")
    void returnsTrueWhenPostReportExists() {
        savePostReport();

        assertThat(postReportRepository.existsByPost_PostNumAndUserInfo_ProfileId(
                post.getPostNum(),
                reporter.getProfileId()
        )).isTrue();
    }

    private UserInfo saveUser(String email, String nickname) {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo(email, "encoded-password")
        );
        return userInfoRepository.save(
                new UserInfo(signInfo, nickname, null)
        );
    }

    private void savePostReport() {
        postReportRepository.saveAndFlush(
                new PostReport(post, reporter)
        );
    }
}
