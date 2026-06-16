package com.example.community.domain.token;


public record TokenDTO(
        long userNum,
        String token
){
    public static TokenDTO from(Token token){
        return new TokenDTO(token.getUserNum(), token.getToken());
    }

}
