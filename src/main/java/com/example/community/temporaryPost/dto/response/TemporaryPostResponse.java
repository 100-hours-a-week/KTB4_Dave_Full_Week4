package com.example.community.temporaryPost.dto.response;

import com.example.community.temporaryPost.entity.TemporaryPost;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record TemporaryPostResponse(
        String title,
        String content,
        String image,
        OffsetDateTime writeAt
) {
    public static TemporaryPostResponse from(TemporaryPost temporaryPost){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new TemporaryPostResponse(
                temporaryPost.getTitle(),
                temporaryPost.getContent(),
                temporaryPost.getImage(),
                temporaryPost.getWriteAt().atOffset(kstOffset)
        );
    }
}
