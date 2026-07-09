package com.example.community.comment.dto.response;

import com.example.community.comment.entity.CommentEditRecord;

import java.time.Instant;

public record CommentEditResponse(
        long editId,
        int version,
        String content,
        Instant writeAt
) {
    public static CommentEditResponse from(CommentEditRecord commentEditRecord){
        return new CommentEditResponse(
                commentEditRecord.getEditId(),
                commentEditRecord.getVersion(),
                commentEditRecord.getContent(),
                commentEditRecord.getWriteAt()
        );
    }
}
