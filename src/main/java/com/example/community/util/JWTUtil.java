package com.example.community.util;

import com.example.community.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JWTUtil {
    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final String CLAIM_PROFILE_ID = "profileID";
    private final String CLAIM_ROLE = "role";
    private final String CLAIM_TYPE = "type";
    private final String TOKEN_TYPE_ACCESS = "access";
    private final String TOKEN_TYPE_REFRESH = "refresh";


    public JWTUtil(@Value("${spring.jwt.secret}") String secret, @Value("${tokenExpire.access}") long accessExpiration, @Value("${tokenExpire.refresh}") long refreshExpiration){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(Long userNum, Long profileId, UserRole role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userNum))
                .claim(CLAIM_PROFILE_ID, profileId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateAccessToken(Long userNum, Long profileId, UserRole role, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userNum))
                .claim(CLAIM_PROFILE_ID, profileId)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userNum) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userNum))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserNumFromToken(String token) {
        Claims claims = validateToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public Long getProfileIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get(CLAIM_PROFILE_ID, Long.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims.get(CLAIM_TYPE, String.class);
    }

    public Date getExpiration(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration();
    }

    public long getExpiresIn(String token) {
        Date expiration = validateToken(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
