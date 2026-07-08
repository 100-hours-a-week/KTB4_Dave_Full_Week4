package com.example.community.user.dto.response;

import com.example.community.user.dto.UserDTO;
import com.example.community.user.entity.SignInfo;

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
