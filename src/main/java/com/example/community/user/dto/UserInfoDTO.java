package com.example.community.user.dto;


import com.example.community.user.dto.request.UserInfoRequest;
import com.example.community.user.entity.UserInfo;
import com.example.community.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
public class UserInfoDTO {
    Long userNum;
    Long profileId;
    String email;
    String nickname;
    String profileImage;
    UserRole userRole;
    Instant deletedAt;

    public void update(UserInfoRequest userInfoRequest){
        this.nickname = userInfoRequest.nickname();
    }

    public static UserInfoDTO from(UserInfo userInfo){
        return new UserInfoDTO(userInfo.getSignInfo().getUserNum() ,userInfo.getProfileId(), null, userInfo.getNickname(), userInfo.getProfileImage(), userInfo.getRole(), userInfo.getDeletedAt());
    }

}
