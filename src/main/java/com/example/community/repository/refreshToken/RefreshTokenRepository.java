package com.example.community.repository.refreshToken;

import com.example.community.domain.token.TokenDTO;

public interface RefreshTokenRepository {
    void addRefreshToken(TokenDTO token);
    boolean checkRefreshToken(TokenDTO token);
    void updateRefreshToken(TokenDTO token);
    void deleteRefreshToken(String token);
    void deleteRefreshToken(long userNum);
}
