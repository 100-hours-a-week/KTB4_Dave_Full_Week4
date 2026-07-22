package com.example.community.auth.service;

import com.example.community.auth.dto.response.RefreshTokenDTO;
import com.example.community.auth.entity.RefreshToken;
import com.example.community.auth.repository.RefreshTokenRepository;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.user.entity.SignInfo;
import com.example.community.user.repository.SignInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final SignInfoRepository signInfoRepository;

    @Transactional
    public void addRefreshToken(long userNum, String token) {
        SignInfo signInfo = signInfoRepository.findByUserNum(userNum)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 유저"));
        RefreshToken refreshToken = new RefreshToken(null, signInfo, token);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshTokenDTO getRefreshToken(String token){
        return RefreshTokenDTO.from(refreshTokenRepository.findByToken(token)
                .orElseThrow(()->new NotFoundException("유효하지 않은 리프레시 토큰")));
    }

    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}
