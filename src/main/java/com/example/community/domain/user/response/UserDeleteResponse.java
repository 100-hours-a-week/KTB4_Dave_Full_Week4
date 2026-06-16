package com.example.community.domain.user.response;

import com.example.community.domain.user.UserDTO;

public record UserDeleteResponse(
        long userNum,
        boolean deleted
) {
    public static UserDeleteResponse from(UserDTO user){
        return new UserDeleteResponse(user.getUserNum(), user.isDeleted());
    }
}
