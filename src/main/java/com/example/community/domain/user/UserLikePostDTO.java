package com.example.community.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class UserLikePostDTO {
    private long userNum;
    private long postNum;

    public boolean equals(UserLikePostDTO userLikePost){
        return userNum == userLikePost.userNum && postNum == userLikePost.postNum;
    }
}
