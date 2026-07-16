package com.example.community.auth.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken
) {
}
