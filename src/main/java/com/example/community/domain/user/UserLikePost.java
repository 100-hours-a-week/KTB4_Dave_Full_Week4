package com.example.community.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class UserLikePost {
    private long userNum;
    private long postNum;

    public boolean equals(UserLikePost userLikePost){
        return userNum == userLikePost.userNum && postNum == userLikePost.postNum;
    }
}
