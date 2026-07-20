package com.example.community.auth.service;

import com.example.community.auth.dto.response.AuthResponse;
import com.example.community.auth.dto.response.RefreshTokenDTO;
import com.example.community.handler.exception.BadRequestException;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.response.SignInResponse;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final RefreshTokenService refreshTokenService;
    private final UserInfoRepository userInfoRepository;
    private final JWTUtil jwtUtil;

    @Transactional
    public AuthResponse refresh(String refreshToken){
        RefreshTokenDTO refresh = refreshTokenService.getRefreshToken(refreshToken);
        SignInfo signInfo = refresh.signInfo();

        if(signInfo.isDeleted()){
            throw new BadRequestException("이미 탈퇴한 유저");
        }

        UserInfo userInfo = userInfoRepository.findBySignInfo_UserNum(signInfo.getUserNum())
                .getFirst();

        String access = jwtUtil.generateAccessToken(signInfo.getUserNum(), userInfo.getProfileId(), userInfo.getRole());
        String newRefresh = jwtUtil.generateRefreshToken(signInfo.getUserNum());
        refreshTokenService.deleteRefreshToken(refreshToken);
        refreshTokenService.addRefreshToken(signInfo.getUserNum(), newRefresh);
        return new AuthResponse(newRefresh, SignInResponse.of(UserInfoDTO.from(userInfo), access));
    }

    @Transactional
    public AuthResponse tokenIssue(UserInfoDTO userInfoDTO){
        String accessToken = jwtUtil.generateAccessToken(userInfoDTO.getUserNum(), userInfoDTO.getProfileId(), userInfoDTO.getUserRole());
        String refreshToken = jwtUtil.generateRefreshToken(userInfoDTO.getUserNum());
        refreshTokenService.addRefreshToken(userInfoDTO.getUserNum(), refreshToken);

        return new AuthResponse(refreshToken, SignInResponse.of(userInfoDTO, accessToken));
    }
}
