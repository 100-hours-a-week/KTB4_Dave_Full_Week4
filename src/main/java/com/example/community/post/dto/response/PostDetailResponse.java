package com.example.community.post.dto.response;

import com.example.community.post.dto.query.PostBodyData;
import com.example.community.post.dto.query.PostStateData;
import com.example.community.util.ImageUrlBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostDetailResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        String content,
        String image,
        String objectKey,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean blind,
        boolean isEdited,
        OffsetDateTime writeAt
) {
    public static PostDetailResponse from(
            PostBodyData body,
            PostStateData state,
            ImageUrlBuilder imageUrlBuilder
    ) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostDetailResponse(
                body.postNum(),
                body.displayNickname(),
                body.displayProfileImage(),
                body.title(),
                body.content(),
                imageUrlBuilder.build(body.imageObjectKey()),
                body.imageObjectKey(),
                state.viewCount(),
                state.likeCount(),
                state.commentCount(),
                state.isBlind(),
                body.editedAt() != null,
                body.writeAt().atOffset(kstOffset)
        );
    }
}
