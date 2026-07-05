package com.example.community.user.dto.response;

import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.entity.UserRole;

public record SignInResponse (
        long userNum,
        long profileId,
        String email,
        String nickname,
        String profileImage,
        UserRole userRole,
        String accessToken
){
    public static SignInResponse of(UserInfoDTO userInfoDTO, String accessToken){
        return new SignInResponse(
                userInfoDTO.getUserNum(), userInfoDTO.getProfileId(), userInfoDTO.getEmail(), userInfoDTO.getNickname(),
                userInfoDTO.getProfileImage(), userInfoDTO.getUserRole(), accessToken
        );
    }
}
