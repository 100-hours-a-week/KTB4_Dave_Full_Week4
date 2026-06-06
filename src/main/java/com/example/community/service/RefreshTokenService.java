package com.example.community.service;

import com.example.community.domain.token.Token;

public interface RefreshTokenService {
    void addRefreshToken();
    boolean checkRefreshToken(Token token);
    void deleteRefreshToken(Token token);
    void deleteRefreshToken(long userNum);
}
