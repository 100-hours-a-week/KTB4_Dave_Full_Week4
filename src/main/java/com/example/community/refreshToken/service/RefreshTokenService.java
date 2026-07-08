package com.example.community.refreshToken.service;

public interface RefreshTokenService {
    void addRefreshToken(long userNum, String token);
    void deleteRefreshToken(String token);
//    void deleteRefreshToken(long userNum);
}
