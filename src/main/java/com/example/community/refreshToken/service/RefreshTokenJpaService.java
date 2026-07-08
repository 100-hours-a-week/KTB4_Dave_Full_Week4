package com.example.community.refreshToken.service;

import com.example.community.handler.exception.NotFoundException;
import com.example.community.refreshToken.dto.TokenDTO;
import com.example.community.refreshToken.entity.RefreshToken;
import com.example.community.refreshToken.repository.RefreshTokenJpaRepository;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.repository.SignInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenJpaService implements RefreshTokenService{
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final SignInfoRepository signInfoRepository;

    @Override
    public void addRefreshToken(long userNum, String token) {
        SignInfo signInfo = signInfoRepository.findByUserNum(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        RefreshToken refreshToken = new RefreshToken(null, signInfo, token);
        refreshTokenJpaRepository.save(refreshToken);
    }

    @Override
    public void deleteRefreshToken(String token) {
        refreshTokenJpaRepository.deleteByToken(token);
    }

//    @Override
//    public void deleteRefreshToken(long userNum) {
//        RefreshToken refreshToken = refreshTokenRepository.findBySignInfo_UserNum(userNum)
//                .orElseThrow(() -> new NotFoundException("존재하지 않는 토큰"));
//
//        refreshTokenRepository.delete(refreshToken);
//    }
}
