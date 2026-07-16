package com.example.community.auth.dto;


import com.example.community.auth.entity.RefreshToken;

public record TokenDTO(
        long userNum,
        String jwtToken
){
    public static TokenDTO from(RefreshToken refreshToken){
        return new TokenDTO(refreshToken.getSignInfo().getUserNum(), refreshToken.getToken());
    }

}
