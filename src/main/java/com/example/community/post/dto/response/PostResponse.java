package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import com.example.community.util.ImageUrlBuilder;

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
        int commentCount,
        boolean blind,
        boolean isEdited,
        OffsetDateTime writeAt
) {

    public static PostResponse from(
            Post post,
            ImageUrlBuilder imageUrlBuilder
    ) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                imageUrlBuilder.build(post.getUserInfo().getProfileImage()),
                post.getMaskedTitle(),
                post.getContent(),
                imageUrlBuilder.build(post.getImage()),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getCommentCount(),
                post.isBlind(),
                post.getEditedAt() != null,
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
