package com.example.community.refreshToken.service;

import com.example.community.refreshToken.dto.TokenDTO;
import com.example.community.refreshToken.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenJsonService implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenJsonService(@Qualifier("refreshTokenJsonRepository") RefreshTokenRepository refreshTokenRepository){
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void addRefreshToken(long userNum, String token) {
        TokenDTO newToken = new TokenDTO(userNum, token);
        refreshTokenRepository.addRefreshToken(newToken);
    }

    @Override
    public boolean checkRefreshToken(long userNum, String token) {
        TokenDTO newToken = new TokenDTO(userNum, token);
        return refreshTokenRepository.checkRefreshToken(newToken);
    }

    @Override
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteRefreshToken(token);
    }

    @Override
    public void deleteRefreshToken(long userNum) {
        refreshTokenRepository.deleteRefreshToken(userNum);
    }
}
