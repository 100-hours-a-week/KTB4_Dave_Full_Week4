package com.example.community.refreshToken.dto;


import com.example.community.refreshToken.entity.RefreshToken;

public record TokenDTO(
        long userNum,
        String jwtToken
){
    public static TokenDTO from(RefreshToken refreshToken){
        return new TokenDTO(refreshToken.getSignInfo().getUserNum(), refreshToken.getToken());
    }

}
