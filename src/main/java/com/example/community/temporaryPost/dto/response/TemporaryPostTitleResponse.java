package com.example.community.temporaryPost.dto.response;

import com.example.community.temporaryPost.entity.TemporaryPost;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record TemporaryPostTitleResponse(
        Long temporaryPostId,
        String title,
        OffsetDateTime writeAt
) {
    public static TemporaryPostTitleResponse from(TemporaryPost temporaryPost){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");

        return new TemporaryPostTitleResponse(
                temporaryPost.getTemporaryId(),
                temporaryPost.getTitle(),
                temporaryPost.getWriteAt().atOffset(kstOffset)
        );
    }
}
