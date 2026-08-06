package com.example.community.temporaryPost.repository;

import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.SignInfoRepository;
import com.example.community.user.repository.UserInfoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TemporaryPostRepositoryTest {

    @Autowired
    private TemporaryPostRepository temporaryPostRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private EntityManager entityManager;

    private UserInfo owner;
    private UserInfo otherUser;

    @BeforeEach
    void setUp() {
        owner = saveUser("owner@example.com", "owner");
        otherUser = saveUser("other@example.com", "other");
    }

    @Test
    @DisplayName("존재하지 않는 임시저장글 번호를 조회하면 빈 Optional을 반환한다")
    void findByTemporaryIdReturnsEmptyWhenTemporaryPostDoesNotExist() {
        assertThat(temporaryPostRepository.findByTemporaryId(Long.MAX_VALUE))
                .isEmpty();
    }

    @Test
    @DisplayName("임시저장글 번호로 저장된 글을 조회한다")
    void findByTemporaryIdReturnsTemporaryPost() {
        TemporaryPost savedPost = saveTemporaryPost(
                owner,
                "temporary-title",
                "temporary-content"
        );
        Long temporaryId = savedPost.getTemporaryId();
        entityManager.flush();
        entityManager.clear();

        assertThat(temporaryPostRepository.findByTemporaryId(temporaryId))
                .hasValueSatisfying(foundPost -> {
                    assertThat(foundPost.getTemporaryId()).isEqualTo(temporaryId);
                    assertThat(foundPost.getUserInfo().getProfileId())
                            .isEqualTo(owner.getProfileId());
                    assertThat(foundPost.getTitle()).isEqualTo("temporary-title");
                    assertThat(foundPost.getContent()).isEqualTo("temporary-content");
                });
    }

    @Test
    @DisplayName("임시저장글이 없는 사용자를 조회하면 빈 목록을 반환한다")
    void findByProfileIdReturnsEmptyWhenUserHasNoTemporaryPosts() {
        assertThat(temporaryPostRepository.findByUserInfo_ProfileId(
                owner.getProfileId()
        )).isEmpty();
    }

    @Test
    @DisplayName("프로필 번호로 해당 사용자의 임시저장글만 조회한다")
    void findByProfileIdReturnsOnlyUsersTemporaryPosts() {
        TemporaryPost firstPost = saveTemporaryPost(owner, "first", "content-1");
        TemporaryPost secondPost = saveTemporaryPost(owner, "second", "content-2");
        saveTemporaryPost(otherUser, "other", "other-content");
        entityManager.flush();
        entityManager.clear();

        List<TemporaryPost> result =
                temporaryPostRepository.findByUserInfo_ProfileId(
                        owner.getProfileId()
                );

        assertThat(result)
                .extracting(TemporaryPost::getTemporaryId)
                .containsExactlyInAnyOrder(
                        firstPost.getTemporaryId(),
                        secondPost.getTemporaryId()
                );
        assertThat(result)
                .allSatisfy(post -> assertThat(post.getUserInfo().getProfileId())
                        .isEqualTo(owner.getProfileId()));
    }

    private UserInfo saveUser(String email, String nickname) {
        SignInfo signInfo = signInfoRepository.save(
                new SignInfo(email, "encoded-password")
        );
        return userInfoRepository.save(
                new UserInfo(signInfo, nickname, null)
        );
    }

    private TemporaryPost saveTemporaryPost(
            UserInfo userInfo,
            String title,
            String content
    ) {
        TemporaryPost temporaryPost = new TemporaryPost(userInfo);
        temporaryPost.update(title, content, null);
        return temporaryPostRepository.save(temporaryPost);
    }
}
