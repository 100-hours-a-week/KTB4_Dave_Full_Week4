package com.example.community.domain.token;

import com.example.community.domain.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record Token (
        long userNum,
        String type,// Access, Refresh
        UserRole role, // ADMIN, USER
        UUID uuid,
        Instant exp
){
    public static boolean isExpired(Token token){
        return Instant.now().isAfter(token.exp());
    }
}
