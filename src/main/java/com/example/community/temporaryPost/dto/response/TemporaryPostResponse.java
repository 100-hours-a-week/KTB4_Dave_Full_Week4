package com.example.community.temporaryPost.dto.response;

import com.example.community.temporaryPost.entity.TemporaryPost;
import com.example.community.util.ImageUrlBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record TemporaryPostResponse(
        String title,
        String content,
        String image,
        String objectKey,
        OffsetDateTime writeAt
) {
    public static TemporaryPostResponse from(
            TemporaryPost temporaryPost,
            ImageUrlBuilder imageUrlBuilder
    ) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new TemporaryPostResponse(
                temporaryPost.getTitle(),
                temporaryPost.getContent(),
                imageUrlBuilder.build(temporaryPost.getImage()),
                temporaryPost.getImage(),
                temporaryPost.getWriteAt().atOffset(kstOffset)
        );
    }
}
