package com.example.community.auth.service;

import com.example.community.auth.dto.response.RefreshTokenDTO;
import com.example.community.handler.exception.NotFoundException;
import com.example.community.auth.entity.RefreshToken;
import com.example.community.auth.repository.RefreshTokenRepository;
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
        // 서비스 계층에서 엔티티를 반환하는 건 안 좋은 구조 DTO로 변경??
        // 근데 사용처는 AuthService인데 AuthService에서 단순하게 모두 refreshRepository로 해결하게?
        return RefreshTokenDTO.from(refreshTokenRepository.findByToken(token)
                .orElseThrow(()->new NotFoundException("유효하지 않은 리프레시 토큰")));
    }

    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

//    @Override
//    public void deleteRefreshToken(long userNum) {
//        RefreshToken refreshToken = refreshTokenRepository.findBySignInfo_UserNum(userNum)
//                .orElseThrow(() -> new NotFoundException("존재하지 않는 토큰"));
//
//        refreshTokenRepository.delete(refreshToken);
//    }
}
