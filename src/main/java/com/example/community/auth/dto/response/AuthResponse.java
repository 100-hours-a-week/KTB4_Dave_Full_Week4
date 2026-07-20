package com.example.community.auth.dto.response;

import com.example.community.user.dto.response.SignInResponse;

public record AuthResponse(
        String refreshToken,
        SignInResponse signInResponse
) {
}
