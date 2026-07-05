package com.example.community.resolver;

import com.example.community.user.entity.UserRole;

public record SignUserInfo(
        Long userNum,
        Long profileId,
        UserRole userRole
) {
}
