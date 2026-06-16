package com.example.community.domain.user.response;

import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserInfoDTO;

public record UserInfoResponse(
        String nickname,
        String profileImage
) {
        public static UserInfoResponse from(UserDTO user){
                return new UserInfoResponse(user.getNickname(), user.getProfileImage());
        }

        public static UserInfoResponse from(UserInfoDTO userInfoDTO){
                return new UserInfoResponse(userInfoDTO.nickname(), userInfoDTO.profileImage());
        }
}
