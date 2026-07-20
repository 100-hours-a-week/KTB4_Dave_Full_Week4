package com.example.community.auth.dto.response;

import com.example.community.auth.entity.RefreshToken;
import com.example.community.user.entity.SignInfo;

public record RefreshTokenDTO(
        Long refreshId,
        SignInfo signInfo,
        String token
) {
    public static RefreshTokenDTO from(RefreshToken refreshToken){
        return new RefreshTokenDTO(
                refreshToken.getRefreshId(),
                refreshToken.getSignInfo(),
                refreshToken.getToken());
    }
}
