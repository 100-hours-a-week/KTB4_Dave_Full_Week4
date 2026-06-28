package com.example.community.domain.post.response;

import com.example.community.domain.post.PostDTO;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostTitleResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        int viewCount,
        int likeCount,
        int reportCount,
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
                post.getViewCount(),
                post.getLikeCount(),
                post.getReportCount(),
                post.getCommentCount(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
    public static PostTitleResponse from(PostDTO post, UserInfoDTO userInfoDTO){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                post.getPostNum(),
                userInfoDTO.nickname(),
                userInfoDTO.profileImage(),
                post.getTitle(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getReportCount(),
                post.getCommentCount(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
