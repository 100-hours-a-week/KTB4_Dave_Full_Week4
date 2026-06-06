package com.example.community.util;

import com.example.community.domain.token.Token;
import com.example.community.domain.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class TokenProvider {
    private final long accessExpiration;
    private final long refreshExpiration;

    public TokenProvider(@Value("${tokenExpire.access}") long accessExpiration, @Value("${tokenExpire.refresh}") long refreshExpiration){
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public Token createAccessToken(long userNum, UserRole userRole){
        return new Token(userNum,"Access", userRole, UUID.randomUUID(), Instant.now().plusMillis(accessExpiration));
    }

    public Token createRefreshToken(long userNum,UserRole userRole){
        return new Token(userNum, "Refresh", userRole, UUID.randomUUID(), Instant.now().plusMillis(refreshExpiration));
    }

}
