package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import com.example.community.user.entity.UserLikePost;

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
    public static PostTitleResponse from(UserLikePost userLikePost){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                userLikePost.getPost().getPostNum(),
                userLikePost.getUserInfo().getNickname(),
                userLikePost.getUserInfo().getProfileImage(),
                userLikePost.getPost().getTitle(),
                userLikePost.getPost().getViewCount(),
                userLikePost.getPost().getLikeCount(),
                userLikePost.getPost().getReportCount(),
                userLikePost.getPost().getCommentCount(),
                userLikePost.getPost().getWriteAt().atOffset(kstOffset)
        );
    }
    public static PostTitleResponse from(Post post){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                post.getUserInfo().getProfileImage(),
                post.getTitle(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getReportCount(),
                post.getCommentCount(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
