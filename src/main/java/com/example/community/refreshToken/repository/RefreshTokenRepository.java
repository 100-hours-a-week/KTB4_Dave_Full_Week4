package com.example.community.refreshToken.repository;

import com.example.community.refreshToken.dto.TokenDTO;

public interface RefreshTokenRepository {
    void addRefreshToken(TokenDTO token);
    boolean checkRefreshToken(TokenDTO token);
    void updateRefreshToken(TokenDTO token);
    void deleteRefreshToken(String token);
    void deleteRefreshToken(long userNum);
}
