package com.example.community.util;

import com.example.community.auth.dto.AccessTokenClaims;
import com.example.community.user.entity.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JWTUtilTest {
    private static final String SECRET =
            "test-secret-key-must-be-at-least-thirty-two-bytes-long";
    private static final long ACCESS_EXPIRATION = 60_000L;
    private static final long REFRESH_EXPIRATION = 120_000L;

    private final JWTUtil jwtUtil = new JWTUtil(
            SECRET,
            ACCESS_EXPIRATION,
            REFRESH_EXPIRATION
    );

    @Test
    @DisplayName("기본 만료 시간으로 액세스 토큰을 생성하고 검증한다")
    void generateAccessTokenCreatesValidToken() {
        String token = jwtUtil.generateAccessToken(
                1L,
                2L,
                UserRole.USER
        );

        Claims claims = jwtUtil.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("profileID", Long.class)).isEqualTo(2L);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime()).isEqualTo(ACCESS_EXPIRATION);
    }

    @Test
    @DisplayName("지정한 만료 시간으로 액세스 토큰을 생성한다")
    void generateAccessTokenUsesRequestedExpiration() {
        String token = jwtUtil.generateAccessToken(
                1L,
                2L,
                UserRole.ADMIN,
                10_000L
        );

        Claims claims = jwtUtil.validateToken(token);

        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime()).isEqualTo(10_000L);
        assertThat(jwtUtil.getExpiresIn(token))
                .isPositive()
                .isLessThanOrEqualTo(10_000L);
    }

    @Test
    @DisplayName("리프레시 토큰은 식별자와 리프레시 타입을 포함해 생성한다")
    void generateRefreshTokenCreatesValidToken() {
        String token = jwtUtil.generateRefreshToken(1L);

        Claims claims = jwtUtil.validateToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
        assertThat(claims.get("profileID")).isNull();
        assertThat(claims.get("role")).isNull();
        assertThat(claims.getId()).isNotBlank();
        assertThatCode(() -> UUID.fromString(claims.getId()))
                .doesNotThrowAnyException();
        assertThat(claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime()).isEqualTo(REFRESH_EXPIRATION);
    }

    @Test
    @DisplayName("유효한 액세스 토큰을 파싱하면 필수 클레임을 반환한다")
    void parseAccessTokenReturnsRequiredClaims() {
        String token = jwtUtil.generateAccessToken(1L, 2L, UserRole.USER);

        AccessTokenClaims claims = jwtUtil.parseAccessToken(token);

        assertThat(claims).isEqualTo(
                new AccessTokenClaims(1L, 2L, UserRole.USER)
        );
    }

    @Test
    @DisplayName("토큰 조회 메서드는 액세스 토큰의 클레임을 반환한다")
    void tokenClaimReadersReturnAccessTokenClaims() {
        String token = jwtUtil.generateAccessToken(1L, 2L, UserRole.ADMIN);
        Claims claims = jwtUtil.validateToken(token);

        assertThat(jwtUtil.getUserNumFromToken(token)).isEqualTo(1L);
        assertThat(jwtUtil.getProfileIdFromToken(token)).isEqualTo(2L);
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.getTokenType(token)).isEqualTo("access");
        assertThat(jwtUtil.getExpiration(token))
                .isEqualTo(claims.getExpiration());
        assertThat(jwtUtil.getExpiresIn(token))
                .isPositive()
                .isLessThanOrEqualTo(ACCESS_EXPIRATION);
    }

    @Test
    @DisplayName("토큰 타입 판별 메서드는 액세스와 리프레시 토큰을 구분한다")
    void tokenTypeChecksDistinguishAccessAndRefreshTokens() {
        String accessToken = jwtUtil.generateAccessToken(
                1L,
                2L,
                UserRole.USER
        );
        String refreshToken = jwtUtil.generateRefreshToken(1L);

        assertThat(jwtUtil.isAccessToken(accessToken)).isTrue();
        assertThat(jwtUtil.isRefreshToken(accessToken)).isFalse();
        assertThat(jwtUtil.isAccessToken(refreshToken)).isFalse();
        assertThat(jwtUtil.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("만료된 리프레시 토큰의 타입 판별은 만료 예외를 던진다")
    void refreshTokenTypeCheckRejectsExpiredToken() {
        JWTUtil expiredTokenUtil = new JWTUtil(
                SECRET,
                ACCESS_EXPIRATION,
                -1L
        );
        String expiredRefreshToken =
                expiredTokenUtil.generateRefreshToken(1L);

        assertThatThrownBy(() ->
                expiredTokenUtil.isRefreshToken(expiredRefreshToken)
        ).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("유효하고 만료되지 않은 토큰은 만료 상태가 아니다")
    void isTokenExpiredReturnsFalseForValidToken() {
        String token = jwtUtil.generateAccessToken(1L, 2L, UserRole.USER);

        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("만료되거나 형식이 잘못된 토큰은 만료 상태로 처리한다")
    void isTokenExpiredReturnsTrueForExpiredOrMalformedToken() {
        String expiredToken = jwtUtil.generateAccessToken(
                1L,
                2L,
                UserRole.USER,
                -1L
        );

        assertThat(jwtUtil.isTokenExpired(expiredToken)).isTrue();
        assertThat(jwtUtil.isTokenExpired("not-a-jwt")).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰을 액세스 토큰으로 파싱하면 예외가 발생한다")
    void parseAccessTokenRejectsRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(1L);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(refreshToken))
                .isInstanceOf(MalformedJwtException.class)
                .hasMessage("Token is not an access token.");
    }

    @Test
    @DisplayName("만료된 액세스 토큰을 파싱하면 만료 예외가 발생한다")
    void parseAccessTokenRejectsExpiredToken() {
        String expiredToken = jwtUtil.generateAccessToken(
                1L,
                2L,
                UserRole.USER,
                -1L
        );

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("missingRequiredClaimCases")
    @DisplayName("필수 클레임이 누락된 액세스 토큰을 거부한다")
    void parseAccessTokenRejectsMissingRequiredClaims(
            String description,
            String subject,
            Long profileId,
            String role
    ) {
        String token = accessToken(subject, profileId, role);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(MalformedJwtException.class)
                .hasMessage("Access token is missing required claims.");
    }

    @Test
    @DisplayName("subject가 공백인 액세스 토큰을 거부한다")
    void parseAccessTokenRejectsBlankSubject() {
        Claims claims = mock(Claims.class);
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn(" ");
        JWTUtil jwtUtilSpy = spy(jwtUtil);
        doReturn(claims).when(jwtUtilSpy).validateToken("blank-subject-token");

        assertThatThrownBy(() -> jwtUtilSpy.parseAccessToken(
                "blank-subject-token"
        )).isInstanceOf(MalformedJwtException.class)
                .hasMessage("Access token is missing required claims.");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidClaimCases")
    @DisplayName("형식이 잘못된 필수 클레임을 거부한다")
    void parseAccessTokenRejectsInvalidClaims(
            String description,
            String subject,
            String role
    ) {
        String token = accessToken(subject, 2L, role);

        assertThatThrownBy(() -> jwtUtil.parseAccessToken(token))
                .isInstanceOf(MalformedJwtException.class)
                .hasMessage("Access token contains invalid claims.")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("서명이 다른 토큰은 검증에 실패한다")
    void validateTokenRejectsTokenSignedWithDifferentKey() {
        JWTUtil otherJwtUtil = new JWTUtil(
                "different-secret-key-that-is-also-at-least-thirty-two-bytes",
                ACCESS_EXPIRATION,
                REFRESH_EXPIRATION
        );
        String token = otherJwtUtil.generateAccessToken(
                1L,
                2L,
                UserRole.USER
        );

        assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(JwtException.class);
    }

    @ParameterizedTest(name = "reader={0}")
    @ValueSource(strings = {
            "validateToken",
            "parseAccessToken",
            "getUserNumFromToken",
            "getProfileIdFromToken",
            "getRoleFromToken",
            "getTokenType",
            "getExpiration",
            "getExpiresIn",
            "isAccessToken",
            "isRefreshToken"
    })
    @DisplayName("토큰 조회 메서드는 잘못된 토큰을 거부한다")
    void tokenReadersRejectMalformedToken(String reader) {
        assertThatThrownBy(() -> invokeTokenReader(reader, "not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("숫자가 아닌 subject는 사용자 번호로 변환할 수 없다")
    void getUserNumFromTokenRejectsNonNumericSubject() {
        String token = accessToken("not-number", 2L, "USER");

        assertThatThrownBy(() -> jwtUtil.getUserNumFromToken(token))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("서명 키가 너무 짧으면 JWT 유틸 생성을 거부한다")
    void constructorRejectsWeakSecret() {
        assertThatThrownBy(() -> new JWTUtil("short-secret", 1L, 1L))
                .isInstanceOf(WeakKeyException.class);
    }

    private Object invokeTokenReader(String reader, String token) {
        return switch (reader) {
            case "validateToken" -> jwtUtil.validateToken(token);
            case "parseAccessToken" -> jwtUtil.parseAccessToken(token);
            case "getUserNumFromToken" -> jwtUtil.getUserNumFromToken(token);
            case "getProfileIdFromToken" ->
                    jwtUtil.getProfileIdFromToken(token);
            case "getRoleFromToken" -> jwtUtil.getRoleFromToken(token);
            case "getTokenType" -> jwtUtil.getTokenType(token);
            case "getExpiration" -> jwtUtil.getExpiration(token);
            case "getExpiresIn" -> jwtUtil.getExpiresIn(token);
            case "isAccessToken" -> jwtUtil.isAccessToken(token);
            case "isRefreshToken" -> jwtUtil.isRefreshToken(token);
            default -> throw new IllegalArgumentException(
                    "지원하지 않는 토큰 조회 메서드입니다."
            );
        };
    }

    private String accessToken(
            String subject,
            Long profileId,
            String role
    ) {
        SecretKey secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_EXPIRATION));
        if (subject != null) {
            builder.claim("sub", subject);
        }
        if (profileId != null) {
            builder.claim("profileID", profileId);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        return builder
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private static Stream<Arguments> missingRequiredClaimCases() {
        return Stream.of(
                Arguments.of("subject 없음", null, 2L, "USER"),
                Arguments.of("profileId 없음", "1", null, "USER"),
                Arguments.of("role 없음", "1", 2L, null),
                Arguments.of("role 공백", "1", 2L, " ")
        );
    }

    private static Stream<Arguments> invalidClaimCases() {
        return Stream.of(
                Arguments.of("숫자가 아닌 subject", "not-number", "USER"),
                Arguments.of("존재하지 않는 역할", "1", "UNKNOWN")
        );
    }
}
