package com.example.community.user.dto.response;

public record UserDeleteResponse(
        long userNum,
        boolean deleted
) {
}
