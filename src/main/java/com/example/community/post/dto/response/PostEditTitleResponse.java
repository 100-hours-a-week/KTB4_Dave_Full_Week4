package com.example.community.post.dto.response;

import com.example.community.post.entity.PostEditRecord;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostEditTitleResponse(
        long editId,
        String title,
        int version,
        OffsetDateTime writeAt
) {
    public static PostEditTitleResponse from(PostEditRecord postEditRecord){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostEditTitleResponse(
                postEditRecord.getEditId(),
                postEditRecord.getTitle(),
                postEditRecord.getVersion(),
                postEditRecord.getWriteAt().atOffset(kstOffset)
        );
    }


}
