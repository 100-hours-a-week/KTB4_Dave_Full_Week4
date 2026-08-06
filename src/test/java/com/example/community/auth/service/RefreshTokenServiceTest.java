package com.example.community.auth.service;

import com.example.community.auth.dto.response.RefreshTokenDTO;
import com.example.community.auth.entity.RefreshToken;
import com.example.community.auth.repository.RefreshTokenRepository;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.repository.SignInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long USER_NUM = 1L;
    private static final String TOKEN = "refresh-token";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SignInfoRepository signInfoRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("리프레시 토큰 저장 시 사용자가 없으면 예외가 발생한다")
    void addRefreshTokenFailsWhenUserDoesNotExist() {
        when(signInfoRepository.findByUserNum(USER_NUM))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                refreshTokenService.addRefreshToken(USER_NUM, TOKEN)
        )
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 유저");

        verify(refreshTokenRepository, never()).save(
                org.mockito.ArgumentMatchers.any(RefreshToken.class)
        );
    }

    @Test
    @DisplayName("사용자와 토큰 값이 연결된 리프레시 토큰을 저장한다")
    void addRefreshTokenSavesTokenForUser() {
        SignInfo signInfo = signInfo();
        when(signInfoRepository.findByUserNum(USER_NUM))
                .thenReturn(Optional.of(signInfo));
        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        refreshTokenService.addRefreshToken(USER_NUM, TOKEN);

        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getRefreshId()).isNull();
        assertThat(savedToken.getSignInfo()).isSameAs(signInfo);
        assertThat(savedToken.getToken()).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("저장된 리프레시 토큰이 없으면 예외가 발생한다")
    void getRefreshTokenFailsWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getRefreshToken(TOKEN))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("유효하지 않은 리프레시 토큰");
    }

    @Test
    @DisplayName("저장된 리프레시 토큰을 DTO로 반환한다")
    void getRefreshTokenReturnsDto() {
        SignInfo signInfo = signInfo();
        RefreshToken refreshToken = new RefreshToken(10L, signInfo, TOKEN);
        when(refreshTokenRepository.findByToken(TOKEN))
                .thenReturn(Optional.of(refreshToken));

        RefreshTokenDTO result = refreshTokenService.getRefreshToken(TOKEN);

        assertThat(result).isEqualTo(
                new RefreshTokenDTO(10L, signInfo, TOKEN)
        );
    }

    @Test
    @DisplayName("전달받은 토큰 값을 삭제 대상으로 repository에 전달한다")
    void deleteRefreshTokenDeletesMatchingToken() {
        refreshTokenService.deleteRefreshToken(TOKEN);

        verify(refreshTokenRepository).deleteByToken(TOKEN);
    }

    private SignInfo signInfo() {
        return new SignInfo(
                USER_NUM,
                "auth@example.com",
                "encoded-password",
                null,
                Instant.parse("2026-08-06T00:00:00Z")
        );
    }
}
