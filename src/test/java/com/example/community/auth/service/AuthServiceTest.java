package com.example.community.auth.service;

import com.example.community.auth.dto.response.AuthResponse;
import com.example.community.auth.dto.response.RefreshResponse;
import com.example.community.auth.dto.response.RefreshTokenDTO;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageUrlBuilder;
import com.example.community.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long USER_NUM = 1L;
    private static final long PROFILE_ID = 2L;
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String ACCESS_TOKEN = "access-token";

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CredentialAuthenticator credentialAuthenticator;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private JWTUtil jwtUtil;

    @Spy
    private ImageUrlBuilder imageUrlBuilder = new ImageUrlBuilder(
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    );

    @InjectMocks
    private AuthService authService;

    private SignInfo signInfo;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        signInfo = new SignInfo(
                USER_NUM,
                "auth@example.com",
                "encoded-password",
                null,
                Instant.parse("2026-08-06T00:00:00Z")
        );
        userInfo = new UserInfo(
                PROFILE_ID,
                signInfo,
                "dave",
                "profiles/profile.png",
                UserRole.USER,
                null
        );
    }

    @Test
    @DisplayName("refresh 타입이 아닌 토큰이면 예외가 발생한다")
    void refreshFailsWhenTokenIsNotRefreshToken() {
        when(jwtUtil.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("refresh토큰이 아닙니다.");

        verify(jwtUtil, never()).isTokenExpired(OLD_REFRESH_TOKEN);
        verifyNoInteractions(refreshTokenService, userInfoRepository);
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 예외가 발생한다")
    void refreshFailsWhenTokenIsExpired() {
        when(jwtUtil.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.isTokenExpired(OLD_REFRESH_TOKEN)).thenReturn(true);

        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessage("로그인이 필요합니다.");

        verifyNoInteractions(refreshTokenService, userInfoRepository);
    }

    @Test
    @DisplayName("DB에 저장된 리프레시 토큰이 없으면 예외를 그대로 전달한다")
    void refreshFailsWhenStoredTokenDoesNotExist() {
        when(jwtUtil.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.isTokenExpired(OLD_REFRESH_TOKEN)).thenReturn(false);
        when(refreshTokenService.getRefreshToken(OLD_REFRESH_TOKEN))
                .thenThrow(new NotFoundException("유효하지 않은 리프레시 토큰"));

        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("유효하지 않은 리프레시 토큰");

        verifyNoInteractions(userInfoRepository);
        verify(refreshTokenService, never())
                .deleteRefreshToken(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("탈퇴한 사용자의 리프레시 토큰이면 예외가 발생한다")
    void refreshFailsWhenUserIsDeleted() {
        SignInfo deletedSignInfo = new SignInfo(
                USER_NUM,
                "auth@example.com",
                "encoded-password",
                Instant.parse("2026-08-05T00:00:00Z"),
                Instant.parse("2026-08-06T00:00:00Z")
        );
        stubValidRefreshToken(deletedSignInfo);

        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 탈퇴한 유저");

        verifyNoInteractions(userInfoRepository);
        verify(refreshTokenService, never())
                .deleteRefreshToken(OLD_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 사용자에게 연결된 프로필 정보가 비어 있으면 토큰을 교체하지 않는다")
    void refreshFailsWhenUserInfoDoesNotExist() {
        stubValidRefreshToken(signInfo);
        when(userInfoRepository.findBySignInfo_UserNum(USER_NUM))
                .thenReturn(List.of());

        assertThatThrownBy(() -> authService.refresh(OLD_REFRESH_TOKEN))
                .isInstanceOf(NoSuchElementException.class);

        verify(jwtUtil, never()).generateAccessToken(
                USER_NUM,
                PROFILE_ID,
                UserRole.USER
        );
        verify(refreshTokenService, never())
                .deleteRefreshToken(OLD_REFRESH_TOKEN);
        verify(refreshTokenService, never())
                .addRefreshToken(USER_NUM, NEW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰이면 기존 토큰을 교체하고 새 토큰 쌍을 반환한다")
    void refreshReplacesTokenAndReturnsNewTokenPair() {
        stubValidRefreshToken(signInfo);
        when(userInfoRepository.findBySignInfo_UserNum(USER_NUM))
                .thenReturn(List.of(userInfo));
        when(jwtUtil.generateAccessToken(USER_NUM, PROFILE_ID, UserRole.USER))
                .thenReturn(ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(USER_NUM))
                .thenReturn(NEW_REFRESH_TOKEN);

        RefreshResponse result = authService.refresh(OLD_REFRESH_TOKEN);

        assertThat(result).isEqualTo(
                new RefreshResponse(ACCESS_TOKEN, NEW_REFRESH_TOKEN)
        );
        verify(jwtUtil).generateAccessToken(
                USER_NUM,
                PROFILE_ID,
                UserRole.USER
        );
        InOrder tokenReplacement = inOrder(refreshTokenService);
        tokenReplacement.verify(refreshTokenService)
                .getRefreshToken(OLD_REFRESH_TOKEN);
        tokenReplacement.verify(refreshTokenService)
                .deleteRefreshToken(OLD_REFRESH_TOKEN);
        tokenReplacement.verify(refreshTokenService)
                .addRefreshToken(USER_NUM, NEW_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("로그인 사용자에게 토큰을 발급하고 응답 DTO를 반환한다")
    void signInAuthenticatesAndReturnsStoredTokenResponse() {
        SignInRequest request = new SignInRequest(
                signInfo.getEmail(),
                "plain-password"
        );
        UserInfoDTO userInfoDTO = UserInfoDTO.from(userInfo);
        userInfoDTO.setEmail(signInfo.getEmail());
        when(credentialAuthenticator.authenticate(request))
                .thenReturn(userInfoDTO);
        when(jwtUtil.generateAccessToken(USER_NUM, PROFILE_ID, UserRole.USER))
                .thenReturn(ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(USER_NUM))
                .thenReturn(NEW_REFRESH_TOKEN);

        AuthResponse result = authService.signIn(request);

        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        assertThat(result.signInResponse().userNum()).isEqualTo(USER_NUM);
        assertThat(result.signInResponse().profileId()).isEqualTo(PROFILE_ID);
        assertThat(result.signInResponse().email())
                .isEqualTo(signInfo.getEmail());
        assertThat(result.signInResponse().nickname()).isEqualTo("dave");
        assertThat(result.signInResponse().profileImage()).isEqualTo(
                "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
                        + "profiles/profile.png"
        );
        assertThat(result.signInResponse().objectKey())
                .isEqualTo("profiles/profile.png");
        assertThat(result.signInResponse().userRole())
                .isEqualTo(UserRole.USER);
        assertThat(result.signInResponse().accessToken())
                .isEqualTo(ACCESS_TOKEN);
        verify(refreshTokenService)
                .addRefreshToken(USER_NUM, NEW_REFRESH_TOKEN);
        verify(credentialAuthenticator).authenticate(request);
    }

    @Test
    @DisplayName("로그아웃은 인증 저장소에서 리프레시 토큰을 제거한다")
    void signOutDeletesRefreshToken() {
        authService.signOut(OLD_REFRESH_TOKEN);

        verify(refreshTokenService).deleteRefreshToken(OLD_REFRESH_TOKEN);
    }

    private void stubValidRefreshToken(SignInfo tokenOwner) {
        when(jwtUtil.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtUtil.isTokenExpired(OLD_REFRESH_TOKEN)).thenReturn(false);
        when(refreshTokenService.getRefreshToken(OLD_REFRESH_TOKEN))
                .thenReturn(new RefreshTokenDTO(
                        10L,
                        tokenOwner,
                        OLD_REFRESH_TOKEN
                ));
    }
}
