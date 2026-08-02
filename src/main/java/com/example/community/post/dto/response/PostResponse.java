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

    public static PostResponse from(Post post) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                post.getUserInfo().getProfileImage(),
                post.getMaskedTitle(),
                post.getContent(),
                buildImageUrl(post.getImage()),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getReportCount(),
                post.getPostState().getCommentCount(),
                post.getEditedAt() != null,
                post.getWriteAt().atOffset(kstOffset)
        );
    }

    public static PostResponse adminFrom(Post post){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                post.getUserInfo().getProfileImage(),
                post.getTitle(),
                post.getContent(),
                buildImageUrl(post.getImage()),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getReportCount(),
                post.getPostState().getCommentCount(),
                post.getEditedAt() != null,
                post.getWriteAt().atOffset(kstOffset)
        );
    }

    private static String buildImageUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        return "https://community-925581110470-ap-northeast-2-an.s3.ap-northeast-2.amazonaws.com/" + objectKey;
    }
}
