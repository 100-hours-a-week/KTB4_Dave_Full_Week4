package com.example.community.domain.token;


public record TokenDTO(
        long userNum,
        String jwtToken
){
    public static TokenDTO from(Token token){
        return new TokenDTO(token.getUserInfo().getSignInfo().getUserNum(), token.getToken());
    }

}
