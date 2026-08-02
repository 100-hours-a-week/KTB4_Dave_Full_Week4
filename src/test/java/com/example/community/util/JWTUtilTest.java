package com.example.community.util;

import com.example.community.auth.dto.AccessTokenClaims;
import com.example.community.user.entity.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {
    private static final String SECRET =
            "test-secret-key-must-be-at-least-thirty-two-bytes-long";

    private final JWTUtil jwtUtil = new JWTUtil(SECRET, 60_000L, 60_000L);

    @Test
    @DisplayName("유효한 액세스 토큰을 파싱하면 필수 클레임을 반환한다")
    void parseAccessTokenReturnsRequiredClaims() {
        String token = jwtUtil.generateAccessToken(1L, 2L, UserRole.USER);

        AccessTokenClaims claims = jwtUtil.parseAccessToken(token);

        assertThat(claims.userNum()).isEqualTo(1L);
        assertThat(claims.profileId()).isEqualTo(2L);
        assertThat(claims.role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("리프레시 토큰을 액세스 토큰으로 파싱하면 예외가 발생한다")
    void parseAccessTokenRejectsRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(1L);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(refreshToken))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("not an access token");
    }

    @Test
    @DisplayName("만료된 액세스 토큰을 파싱하면 만료 예외가 발생한다")
    void parseAccessTokenRejectsExpiredToken() {
        String expiredToken = jwtUtil.generateAccessToken(1L, 2L, UserRole.USER, -1L);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
