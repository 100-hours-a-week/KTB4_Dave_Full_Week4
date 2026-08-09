package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record AdminPostTitleResponse(
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
    public static AdminPostTitleResponse from(Post post) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new AdminPostTitleResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                post.getUserInfo().getProfileImage(),
                post.getTitle(),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getReportCount(),
                post.getPostState().getCommentCount(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
