package com.example.community.domain.user.response;

import com.example.community.domain.user.User;

public record UserResponse(
        long userNum,
        String email,
        String nickname,
        String profileImage
) {
        public static UserResponse from(User user){
                return new UserResponse(user.getUserNum(), user.getEmail(), user.getNickname(), user.getProfileImage());
        }
}
