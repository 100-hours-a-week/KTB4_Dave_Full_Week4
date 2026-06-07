package com.example.community.domain.user.response;

import com.example.community.domain.user.User;

public record UserDeleteResponse(
        long userNum,
        boolean deleted
) {
    public static UserDeleteResponse from(User user){
        return new UserDeleteResponse(user.getUserNum(), user.isDeleted());
    }
}
