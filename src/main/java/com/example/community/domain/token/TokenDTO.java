package com.example.community.domain.token;


public record TokenDTO(
        long userNum,
        String jwtToken
){
    public static TokenDTO from(RefreshToken refreshToken){
        return new TokenDTO(refreshToken.getSignInfo().getUserNum(), refreshToken.getToken());
    }

}
