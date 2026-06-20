package com.example.community.domain.user.response;

import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.UserRole;

public record SignInResponse (
        long userNum,
        long profileNum,
        String nickname,
        String profileImage,
        UserRole userRole,
        String accessToken
){
    public static SignInResponse of(UserInfoDTO userInfoDTO, String accessToken){
        return new SignInResponse(
                userInfoDTO.userNum(), userInfoDTO.profileId(), userInfoDTO.nickname(), userInfoDTO.profileImage(),
                userInfoDTO.userRole(), accessToken
        );
    }
}
