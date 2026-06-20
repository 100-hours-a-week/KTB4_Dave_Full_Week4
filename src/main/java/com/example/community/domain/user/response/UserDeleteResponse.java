package com.example.community.domain.user.response;

import com.example.community.domain.user.SignInfo;
import com.example.community.domain.user.UserDTO;

public record UserDeleteResponse(
        long userNum,
        boolean deleted
) {
    public static UserDeleteResponse from(UserDTO user){
        return new UserDeleteResponse(user.getUserNum(), user.isDeleted());
    }

    public static UserDeleteResponse from(SignInfo signInfo){
        return new UserDeleteResponse(signInfo.getUserNum(), signInfo.getDeletedAt() != null);
    }
}
