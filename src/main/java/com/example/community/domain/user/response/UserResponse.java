package com.example.community.domain.user.response;

import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserRole;

public record UserResponse(
        long userNum,
        String email,
        String nickname,
        String profileImage,
        UserRole userRole
) {
        public static UserResponse from(UserDTO user){
                return new UserResponse(user.getUserNum(), user.getEmail(), user.getNickname(), user.getProfileImage(), user.getUserRole());
        }
}
