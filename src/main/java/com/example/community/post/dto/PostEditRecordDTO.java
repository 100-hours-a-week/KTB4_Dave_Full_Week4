package com.example.community.post.dto;

import com.example.community.post.entity.Post;
import com.example.community.post.entity.PostEditRecord;

import java.time.Instant;

public record PostEditRecordDTO(
        long postNum,
        int version,
        String title,
        String content,
        String image,
        Instant writeTime
) {
    public static PostEditRecordDTO from(Post post){
        return new PostEditRecordDTO(
                post.getPostNum(),
                post.getVersion(),
                post.getMaskedTitle(),
                post.getContent(),
                post.getImage(),
                post.getEditedAt() != null ? post.getEditedAt() : post.getWriteAt()
                );
    }
    public static PostEditRecordDTO from(PostEditRecord postEditRecord){
        return new PostEditRecordDTO(
                postEditRecord.getPost().getPostNum(),
                postEditRecord.getVersion(),
                postEditRecord.getTitle(),
                postEditRecord.getContent(),
                postEditRecord.getImage(),
                postEditRecord.getWriteAt()
        );
    }
}
