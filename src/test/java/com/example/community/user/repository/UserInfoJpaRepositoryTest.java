package com.example.community.user.repository;

import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

@DataJpaTest
class UserInfoJpaRepositoryTest {
    @Autowired
    SignInfoJpaRepository signInfoJpaRepository;
    @Autowired
    UserInfoJpaRepository userInfoJpaRepository;

    @Test
    @DisplayName("유저번호로 유저정보 조회")
    void findBySignInfo_UserNum() {
        SignInfo signInfo = new SignInfo("wns1628@gmail.com", "1234");
        UserInfo userInfo = new UserInfo(signInfo, "dave", null);
        signInfoJpaRepository.save(signInfo);
        userInfoJpaRepository.save(userInfo);

        List<UserInfo> result = userInfoJpaRepository.findBySignInfo_UserNum(signInfo.getUserNum());
        assertThat(result).hasSize(1);
        assertThat(result)
                .extracting(UserInfo::getNickname)
                .containsExactly("dave");
    }

    @Test
    void findByNickname() {
    }

    @Test
    void findByProfileId() {
    }

    @Test
    void existsByNickname() {
    }

    @Test
    void findByProfileIdIn() {
    }
}