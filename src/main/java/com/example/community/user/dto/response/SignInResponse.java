package com.example.community.user.dto.response;

import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.entity.UserRole;
import com.example.community.util.ImageUrlBuilder;

public record SignInResponse (
        long userNum,
        long profileId,
        String email,
        String nickname,
        String profileImage,
        String objectKey,
        UserRole userRole,
        String accessToken
){
    public static SignInResponse of(
            UserInfoDTO userInfoDTO,
            String accessToken,
            ImageUrlBuilder imageUrlBuilder
    ) {
        return new SignInResponse(
                userInfoDTO.getUserNum(), userInfoDTO.getProfileId(), userInfoDTO.getEmail(), userInfoDTO.getNickname(),
                imageUrlBuilder.build(userInfoDTO.getProfileImage()),
                userInfoDTO.getProfileImage(),
                userInfoDTO.getUserRole(), accessToken
        );
    }
}
