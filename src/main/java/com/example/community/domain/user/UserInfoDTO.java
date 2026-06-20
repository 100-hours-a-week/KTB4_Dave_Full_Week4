package com.example.community.domain.user;


import java.time.Instant;

public record UserInfoDTO
(
        Long userNum,
        Long profileId,
        String nickname,
        String profileImage,
        UserRole userRole,
        Instant deletedAt
){
    public static UserInfoDTO from(UserDTO user){
        return new UserInfoDTO(user.getUserNum(), user.getUserNum(), user.getNickname(), user.getProfileImage(), user.getUserRole(), user.getDeletedAt());
    }

    public static UserInfoDTO from(UserInfo userInfo){
        return new UserInfoDTO(userInfo.getSignInfo().getUserNum() ,userInfo.getProfileId(), userInfo.getNickname(), userInfo.getProfileImage(), userInfo.getRole(), userInfo.getDeletedAt());
    }
}
