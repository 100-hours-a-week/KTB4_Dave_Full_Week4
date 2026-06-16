package com.example.community.domain.user;


public record UserInfoDTO
(
        Long userNum,
        String nickname,
        String profileImage,
        boolean deleted
){
    public static UserInfoDTO from(UserDTO user){
        return new UserInfoDTO(user.getUserNum(), user.getNickname(), user.getProfileImage(), user.isDeleted());
    }
}
