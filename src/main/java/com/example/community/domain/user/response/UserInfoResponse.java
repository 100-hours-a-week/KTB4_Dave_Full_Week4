package com.example.community.domain.user.response;

import com.example.community.domain.user.User;

public record UserInfoResponse(
        String nickname,
        String profileImage
) {
        public static UserInfoResponse from(User user){
                return new UserInfoResponse(user.getNickname(), user.getProfileImage());
        }
}
