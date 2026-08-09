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
        int commentCount,
        boolean blind,
        OffsetDateTime writeAt
) {
    public static PostTitleResponse from(UserLikePost userLikePost){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                userLikePost.getPost().getPostNum(),
                userLikePost.getUserInfo().getNickname(),
                userLikePost.getUserInfo().getProfileImage(),
                userLikePost.getPost().getMaskedTitle(),
                userLikePost.getPost().getPostState().getViewCount(),
                userLikePost.getPost().getPostState().getLikeCount(),
                userLikePost.getPost().getPostState().getCommentCount(),
                userLikePost.getPost().isBlind(),
                userLikePost.getPost().getWriteAt().atOffset(kstOffset)
        );
    }
    public static PostTitleResponse from(Post post){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                post.getUserInfo().getProfileImage(),
                post.getMaskedTitle(),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getCommentCount(),
                post.isBlind(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
