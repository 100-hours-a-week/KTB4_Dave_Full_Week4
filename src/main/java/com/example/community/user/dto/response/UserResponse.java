package com.example.community.user.dto.response;

import com.example.community.user.dto.UserDTO;
import com.example.community.user.entity.UserRole;

public record UserResponse(
        long userNum,
        long profileId,
        String email,
        String nickname,
        String profileImage,
        UserRole userRole
) {
        public static UserResponse from(UserDTO user) {
                return new UserResponse(user.getUserNum(), user.getProfileId(), user.getEmail(), user.getNickname(), user.getProfileImage(), user.getUserRole());
        }

}
