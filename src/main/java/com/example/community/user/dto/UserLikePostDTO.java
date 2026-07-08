package com.example.community.user.dto;

import com.example.community.user.entity.UserLikePost;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class UserLikePostDTO {
    private long profileId;
    private long postNum;

    public static UserLikePostDTO from(UserLikePost userLikePost){
        return new UserLikePostDTO(userLikePost.getUserInfo().getProfileId(), userLikePost.getPost().getPostNum());
    }

    public boolean equals(UserLikePostDTO userLikePost){
        return profileId == userLikePost.profileId && postNum == userLikePost.postNum;
    }
}
