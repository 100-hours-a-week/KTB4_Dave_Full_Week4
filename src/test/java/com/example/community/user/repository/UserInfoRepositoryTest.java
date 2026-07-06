package com.example.community.user.repository;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserInfoRepositoryTest {
    @Autowired
    private UserInfoRepository userInfoRepository;
    @Autowired
    private SignInfoRepository signInfoRepository;
    private final SignInfo SIGN_INFO =  new SignInfo(null, "wns1628@gmail.com", "!234", null, Instant.now());
    private final UserInfo USER_INFO = new UserInfo(null, SIGN_INFO, "dave", null, UserRole.USER, null);

    @BeforeAll
    static void addUser(){
        
    }
    @BeforeEach
    void init(){
        signInfoRepository.deleteAll();
        userInfoRepository.deleteAll();
        signInfoRepository.save(SIGN_INFO);
    }

    @Test
    void findBySignInfo_UserNum() {
        List<UserInfo> userInfo = userInfoRepository.findBySignInfo_UserNum(1L);
        assertThat(userInfo).isEmpty();
        userInfoRepository.save(USER_INFO);
        UserInfo result = userInfoRepository.findBySignInfo_UserNum(SIGN_INFO.getUserNum()).getFirst();
        assertThat(result).isEqualTo(USER_INFO);
    }

    @Test
    void findByNickname() {
        Optional<UserInfo> userInfo = userInfoRepository.findByNickname(USER_INFO.getNickname());
        assertThat(userInfo).isEmpty();
        userInfoRepository.save(USER_INFO);
        userInfo = userInfoRepository.findByNickname(USER_INFO.getNickname());
        assertThat(userInfo).isPresent();
        assertThat(userInfo.get()).isEqualTo(USER_INFO);
    }

    @Test
    void findByProfileId() {
        Optional<UserInfo> userInfo = userInfoRepository.findByProfileId(0L);
        assertThat(userInfo).isEmpty();
        userInfoRepository.save(USER_INFO);
        userInfo = userInfoRepository.findByProfileId(USER_INFO.getProfileId());
        assertThat(userInfo).isPresent();
        assertThat(userInfo.get()).isEqualTo(USER_INFO);
    }

    @Test
    void existsByNickname() {
        boolean exist = userInfoRepository.existsByNickname(USER_INFO.getNickname());
        assertThat(exist).isFalse();
        userInfoRepository.save(USER_INFO);
        exist = userInfoRepository.existsByNickname(USER_INFO.getNickname());
        assertThat(exist).isTrue();
    }

    @Test
    void findByProfileIdIn() {
        userInfoRepository.save(USER_INFO);
        SignInfo signInfo = new SignInfo(null, "test@test.com", "1234", null, Instant.now());
        signInfoRepository.save(signInfo);
        UserInfo userInfo = new UserInfo(null, signInfo, "test", null, UserRole.USER, null);
        userInfoRepository.save(userInfo);
        List<Long> profileIds = List.of(USER_INFO.getProfileId(), userInfo.getProfileId());

        assertThat(userInfoRepository.findByProfileIdIn(profileIds)).containsExactlyInAnyOrder(USER_INFO, userInfo);
    }
}