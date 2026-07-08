package com.example.community.user.repository;

import com.example.community.user.entity.SignInfo;
import org.junit.jupiter.api.BeforeEach;
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
    void findByEmail() {
        Optional<SignInfo> signInfo = signInfoRepository.findByEmail(SIGN_INFO.getEmail());
        assertThat(signInfo).isEmpty();
        signInfoRepository.save(SIGN_INFO);
        signInfo = signInfoRepository.findByEmail(SIGN_INFO.getEmail());
        assertThat(signInfo).isPresent();
        assertThat(signInfo.get()).isEqualTo(SIGN_INFO);
    }

    @Test
    void findByUserNum() {
        Optional<SignInfo> signInfo = signInfoRepository.findByEmail(SIGN_INFO.getEmail());
        assertThat(signInfo).isEmpty();
        signInfoRepository.save(SIGN_INFO);
        signInfo = signInfoRepository.findByUserNum(SIGN_INFO.getUserNum());
        assertThat(signInfo).isPresent();
        assertThat(signInfo.get()).isEqualTo(SIGN_INFO);
    }

    @Test
    void existsByEmail() {
        boolean exist = signInfoRepository.existsByEmail(SIGN_INFO.getEmail());
        assertThat(exist).isFalse();
        signInfoRepository.save(SIGN_INFO);
        exist = signInfoRepository.existsByEmail(SIGN_INFO.getEmail());
        assertThat(exist).isTrue();
    }
}