package com.example.community.domain.user;


public record UserInfoDTO
(
        Long userNum,
        String nickname,
        String profileImage
){
    public static UserInfoDTO from(User user){
        return new UserInfoDTO(user.getUserNum(), user.getNickname(), user.getProfileImage());
    }
}
