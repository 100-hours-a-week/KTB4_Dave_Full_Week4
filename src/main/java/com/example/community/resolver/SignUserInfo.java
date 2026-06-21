package com.example.community.resolver;

import com.example.community.domain.user.UserRole;

public record SignUserInfo(
        Long userNum,
        Long profileId,
        UserRole userRole
) {
}
