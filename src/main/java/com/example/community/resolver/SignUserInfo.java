package com.example.community.resolver;

import com.example.community.domain.user.UserRole;

public record SignUserInfo(
        Long userNum,
        UserRole userRole
) {
}
