package com.example.community.domain.user.response;

import com.example.community.domain.user.UserDTO;
import com.example.community.domain.user.UserRole;

public record UserResponse(
        long userNum,
        long profileId,
        String email,
        String nickname,
        String profileImage,
        UserRole userRole
) {
        public static UserResponse from(UserDTO user) {
                // JSON으로 개발 당시에는 profileID로 분리 전이기 때문에 profileId와 userNum이 동일
                return new UserResponse(user.getUserNum(), user.getProfileId(), user.getEmail(), user.getNickname(), user.getProfileImage(), user.getUserRole());
        }

}
