package com.example.community.auth.service;

import com.example.community.auth.dto.response.RefreshResponse;
import com.example.community.user.repository.UserInfoRepository;
import com.example.community.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    RefreshTokenService refreshTokenService;
    UserInfoRepository userInfoRepository;
    JWTUtil jwtUtil;

    public RefreshResponse refresh(String refreshToken){


        return new RefreshResponse(null, null);
    }
}
