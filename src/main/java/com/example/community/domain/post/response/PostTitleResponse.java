package com.example.community.domain.post.response;

import com.example.community.domain.post.PostDTO;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.LocalDateTime;

public record PostTitleResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        int view,
        int like,
        int report,
        int numberOfComments,
        LocalDateTime writeTime
) {
    public static PostTitleResponse from(PostDTO post, UserInfoResponse userInfoResponse){
        return new PostTitleResponse(
                post.getPostNum(),
                userInfoResponse.nickname(),
                userInfoResponse.profileImage(),
                post.getTitle(),
                post.getView(),
                post.getLike(),
                post.getReport(),
                post.getNumberOfComments(),
                post.getWriteTime()
        );
    }
}
