package com.example.community.post.dto.response;

import com.example.community.post.dto.PostDTO;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.response.UserInfoResponse;

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
        int commentCount,
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
                userInfoDTO.getNickname(),
                userInfoDTO.getProfileImage(),
                post.getTitle(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getReportCount(),
                post.getCommentCount(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
