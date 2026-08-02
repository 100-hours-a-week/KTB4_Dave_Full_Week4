package com.example.community.auth.dto;

import com.example.community.user.entity.UserRole;

public record AccessTokenClaims(
        Long userNum,
        Long profileId,
        UserRole role
) {
}
