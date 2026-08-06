package com.example.community.user.dto.response;

import com.example.community.user.entity.UserInfo;
import com.example.community.util.ImageUrlBuilder;

public record UserInfoResponse(
        String nickname,
        String profileImage,
        String objectKey
) {
        public static UserInfoResponse from(
                UserInfo userInfo,
                ImageUrlBuilder imageUrlBuilder
        ) {
                return new UserInfoResponse(
                        userInfo.getNickname(),
                        imageUrlBuilder.build(userInfo.getProfileImage()),
                        userInfo.getProfileImage()
                );
        }
}
