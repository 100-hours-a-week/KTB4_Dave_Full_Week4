package com.example.community.auth.service;

import com.example.community.auth.dto.response.AuthResponse;
import com.example.community.auth.dto.response.RefreshResponse;
import com.example.community.auth.dto.response.RefreshTokenDTO;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.handler.exception.UnAuthorizedException;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.request.SignInRequest;
import com.example.community.user.dto.response.SignInResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.ImageUrlBuilder;
import com.example.community.util.JWTUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final CredentialAuthenticator credentialAuthenticator;
    private final UserInfoRepository userInfoRepository;
    private final JWTUtil jwtUtil;
    private final ImageUrlBuilder imageUrlBuilder;

    @Transactional
    public AuthResponse signIn(SignInRequest request) {
        UserInfoDTO userInfo = credentialAuthenticator.authenticate(request);
        String accessToken = jwtUtil.generateAccessToken(
                userInfo.getUserNum(),
                userInfo.getProfileId(),
                userInfo.getUserRole()
        );
        String refreshToken = createAndStoreRefreshToken(
                userInfo.getUserNum()
        );
        return new AuthResponse(
                refreshToken,
                SignInResponse.of(userInfo, accessToken, imageUrlBuilder)
        );
    }

    @Transactional
    public void signOut(String refreshToken) {
        refreshTokenService.deleteRefreshToken(refreshToken);
    }

    @Transactional
    public RefreshResponse refresh(String refreshToken){
        validateRefreshToken(refreshToken);
        RefreshTokenDTO refresh = refreshTokenService.getRefreshToken(refreshToken);
        SignInfo signInfo = refresh.signInfo();

        if(signInfo.isDeleted()){
            throw new BadRequestException("이미 탈퇴한 유저");
        }

        UserInfo userInfo = userInfoRepository.findBySignInfo_UserNum(signInfo.getUserNum())
                .getFirst();

        String access = jwtUtil.generateAccessToken(
                signInfo.getUserNum(),
                userInfo.getProfileId(),
                userInfo.getRole()
        );
        refreshTokenService.deleteRefreshToken(refreshToken);
        String newRefresh = createAndStoreRefreshToken(
                signInfo.getUserNum()
        );
        return new RefreshResponse(access, newRefresh);
    }

    private void validateRefreshToken(String refreshToken) {
        try {
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new BadRequestException("refresh토큰이 아닙니다.");
            }
        } catch (ExpiredJwtException exception) {
            throw new UnAuthorizedException("로그인이 필요합니다.");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadRequestException("유효하지 않은 refresh토큰입니다.");
        }
    }

    private String createAndStoreRefreshToken(long userNum) {
        String refreshToken = jwtUtil.generateRefreshToken(userNum);
        refreshTokenService.addRefreshToken(userNum, refreshToken);
        return refreshToken;
    }
}
