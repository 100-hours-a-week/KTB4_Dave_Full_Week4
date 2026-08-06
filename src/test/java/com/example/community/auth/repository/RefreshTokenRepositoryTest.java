package com.example.community.auth.repository;

import com.example.community.auth.entity.RefreshToken;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.repository.SignInfoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SignInfoRepository signInfoRepository;

    @Autowired
    private EntityManager entityManager;

    private SignInfo signInfo;

    @BeforeEach
    void setUp() {
        signInfo = signInfoRepository.save(
                new SignInfo("auth@example.com", "encoded-password")
        );
    }

    @Test
    @DisplayName("저장되지 않은 리프레시 토큰을 조회하면 빈 Optional을 반환한다")
    void findByTokenReturnsEmptyWhenTokenDoesNotExist() {
        assertThat(refreshTokenRepository.findByToken("missing-token"))
                .isEmpty();
    }

    @Test
    @DisplayName("토큰 값으로 DB에 저장된 리프레시 토큰과 사용자 정보를 조회한다")
    void findByTokenReturnsStoredRefreshToken() {
        RefreshToken savedToken = refreshTokenRepository.save(
                new RefreshToken(null, signInfo, "stored-token")
        );
        Long refreshId = savedToken.getRefreshId();
        Long userNum = signInfo.getUserNum();
        entityManager.flush();
        entityManager.clear();

        assertThat(refreshTokenRepository.findByToken("stored-token"))
                .hasValueSatisfying(foundToken -> {
                    assertThat(foundToken.getRefreshId()).isEqualTo(refreshId);
                    assertThat(foundToken.getToken()).isEqualTo("stored-token");
                    assertThat(foundToken.getSignInfo().getUserNum())
                            .isEqualTo(userNum);
                    assertThat(foundToken.getSignInfo().getEmail())
                            .isEqualTo("auth@example.com");
                });
    }

    @Test
    @DisplayName("삭제할 토큰만 삭제하고 다른 토큰은 유지한다")
    void deleteByTokenDeletesOnlyMatchingToken() {
        refreshTokenRepository.save(
                new RefreshToken(null, signInfo, "delete-target")
        );
        RefreshToken remainingToken = refreshTokenRepository.save(
                new RefreshToken(null, signInfo, "remaining-token")
        );
        Long remainingTokenId = remainingToken.getRefreshId();
        refreshTokenRepository.flush();

        refreshTokenRepository.deleteByToken("delete-target");

        assertThat(refreshTokenRepository.findByToken("delete-target"))
                .isEmpty();
        assertThat(refreshTokenRepository.findByToken("remaining-token"))
                .hasValueSatisfying(foundToken ->
                        assertThat(foundToken.getRefreshId())
                                .isEqualTo(remainingTokenId)
                );
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }
}
