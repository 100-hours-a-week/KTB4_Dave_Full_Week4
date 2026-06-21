package com.example.community.domain.user;

import lombok.*;

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
