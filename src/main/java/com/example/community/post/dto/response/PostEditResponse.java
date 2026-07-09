package com.example.community.post.dto.response;

import com.example.community.post.entity.PostEditRecord;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostEditResponse(
        String title,
        String content,
        String image,
        OffsetDateTime writeAt
) {
    public static PostEditResponse from(PostEditRecord postEditRecord){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostEditResponse(
                postEditRecord.getTitle(),
                postEditRecord.getContent(),
                postEditRecord.getImage(),
                postEditRecord.getWriteAt().atOffset(kstOffset)
        );
    }
}
