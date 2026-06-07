package com.example.community.service;

import com.example.community.domain.token.Token;
import com.example.community.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenJsonService implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenJsonService(@Qualifier("refreshTokenJsonRepository") RefreshTokenRepository refreshTokenRepository){
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void addRefreshToken(Token token) {
        refreshTokenRepository.addRefreshToken(token);
    }

    @Override
    public boolean checkRefreshToken(Token token) {
        return refreshTokenRepository.checkRefreshToken(token);
    }

    @Override
    public void deleteRefreshToken(Token token) {
        refreshTokenRepository.deleteRefreshToken(token);
    }

    @Override
    public void deleteRefreshToken(long userNum) {
        refreshTokenRepository.deleteRefreshToken(userNum);
    }
}
