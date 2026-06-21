package com.example.community.domain.post;

import java.time.Instant;

public record PostEditRecordDTO(
        long postNum,
        int version,
        String title,
        String content,
        String image,
        Instant writeTime
) {
    public static PostEditRecordDTO from(PostDTO post){
        return new PostEditRecordDTO(
                post.getPostNum(),
                post.getVersion(),
                post.getTitle(),
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
