package com.example.community.post.repository;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostView;
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
class PostViewRepositoryTest {

    @Autowired
    private PostViewRepository postViewRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    private Post post;
    private UserInfo viewer;

    @BeforeEach
    void setUp() {
        UserInfo author = saveUser("author@example.com", "author");
        viewer = saveUser("viewer@example.com", "viewer");
        post = postRepository.save(
                new Post(author, "title", "content", null)
        );
    }

    @Test
    @DisplayName("게시글이 없으면 빈 Optional을 반환한다")
    void returnsEmptyWhenPostDoesNotExist() {
        savePostView();

        assertThat(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                Long.MAX_VALUE,
                viewer.getProfileId()
        )).isEmpty();
    }

    @Test
    @DisplayName("사용자가 없으면 빈 Optional을 반환한다")
    void returnsEmptyWhenUserDoesNotExist() {
        savePostView();

        assertThat(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                post.getPostNum(),
                Long.MAX_VALUE
        )).isEmpty();
    }

    @Test
    @DisplayName("게시글과 사용자가 존재해도 조회 이력이 없으면 빈 Optional을 반환한다")
    void returnsEmptyWhenPostViewDoesNotExist() {
        assertThat(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                post.getPostNum(),
                viewer.getProfileId()
        )).isEmpty();
    }

    @Test
    @DisplayName("게시글과 사용자의 조회 이력이 있으면 해당 이력을 반환한다")
    void returnsPostViewWhenItExists() {
        PostView postView = savePostView();

        assertThat(postViewRepository.findByPost_PostNumAndUserInfo_ProfileId(
                post.getPostNum(),
                viewer.getProfileId()
        )).contains(postView);
    }

    private UserInfo saveUser(String email, String nickname) {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo(email, "encoded-password")
        );
        return userInfoRepository.save(
                new UserInfo(signInfo, nickname, null)
        );
    }

    private PostView savePostView() {
        return postViewRepository.saveAndFlush(
                new PostView(post, viewer)
        );
    }
}
