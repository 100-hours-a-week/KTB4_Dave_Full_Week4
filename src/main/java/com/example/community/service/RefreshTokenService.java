package com.example.community.service;

public interface RefreshTokenService {
    void addRefreshToken(long userNum, String token);
    boolean checkRefreshToken(long userNum,String token);
    void deleteRefreshToken(String token);
    void deleteRefreshToken(long userNum);
}
