package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        String content,
        String image,
        int viewCount,
        int likeCount,
        int reportCount,
        int commentCount,
        boolean isEdited,
        OffsetDateTime writeAt
) {

    public static PostResponse from(Post post){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                post.getUserInfo().getProfileImage(),
                post.getTitle(),
                post.getContent(),
                post.getImage(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getReportCount(),
                post.getCommentCount(),
                post.getEditedAt() != null,
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
