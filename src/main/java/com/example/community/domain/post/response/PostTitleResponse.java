package com.example.community.domain.post.response;

import com.example.community.domain.post.PostDTO;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostTitleResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        int view,
        int like,
        int report,
        int numberOfComments,
        OffsetDateTime writeAt
) {
    public static PostTitleResponse from(PostDTO post, UserInfoResponse userInfoResponse){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                post.getPostNum(),
                userInfoResponse.nickname(),
                userInfoResponse.profileImage(),
                post.getTitle(),
                post.getView(),
                post.getLike(),
                post.getReport(),
                post.getNumberOfComments(),
                post.getWriteTime().atOffset(kstOffset)
        );
    }
}
