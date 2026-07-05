package com.example.community.user.dto.response;

import com.example.community.user.dto.UserDTO;
import com.example.community.user.dto.UserInfoDTO;

public record UserInfoResponse(
        String nickname,
        String profileImage
) {
        public static UserInfoResponse from(UserDTO user){
                return new UserInfoResponse(user.getNickname(), user.getProfileImage());
        }

        public static UserInfoResponse from(UserInfoDTO userInfoDTO){
                return new UserInfoResponse(userInfoDTO.getNickname(), userInfoDTO.getProfileImage());
        }
}
