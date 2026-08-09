package com.example.community.user.repository;

import com.example.community.user.entity.SignInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SignInfoRepositoryTest {
    @Autowired
    private SignInfoRepository signInfoRepository;
    private final SignInfo SIGN_INFO =  new SignInfo(null, "wns1628@gmail.com", "!234", null, Instant.now());


    @Test
    @DisplayName("이메일로 로그인 정보를 조회한다")
    void findByEmail() {
        Optional<SignInfo> signInfo = signInfoRepository.findByEmail(SIGN_INFO.getEmail());
        assertThat(signInfo).isEmpty();
        signInfoRepository.save(SIGN_INFO);
        signInfo = signInfoRepository.findByEmail(SIGN_INFO.getEmail());
        assertThat(signInfo).isPresent();
        assertThat(signInfo.get()).isEqualTo(SIGN_INFO);
    }

    @Test
    @DisplayName("회원 번호로 로그인 정보를 조회한다")
    void findByUserNum() {
        Optional<SignInfo> signInfo = signInfoRepository.findByEmail(SIGN_INFO.getEmail());
        assertThat(signInfo).isEmpty();
        signInfoRepository.save(SIGN_INFO);
        signInfo = signInfoRepository.findByUserNum(SIGN_INFO.getUserNum());
        assertThat(signInfo).isPresent();
        assertThat(signInfo.get()).isEqualTo(SIGN_INFO);
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인한다")
    void existsByEmail() {
        boolean exist = signInfoRepository.existsByEmail(SIGN_INFO.getEmail());
        assertThat(exist).isFalse();
        signInfoRepository.save(SIGN_INFO);
        exist = signInfoRepository.existsByEmail(SIGN_INFO.getEmail());
        assertThat(exist).isTrue();
    }
}
