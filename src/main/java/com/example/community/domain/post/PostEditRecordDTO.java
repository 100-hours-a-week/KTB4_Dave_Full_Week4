package com.example.community.domain.post;

import java.time.Instant;

public record PostEditRecordDTO(
        long postNum,
        long userNum,
        String title,
        String content,
        String image,
        Instant writeTime
) {
    public static PostEditRecordDTO from(PostDTO post){
        return new PostEditRecordDTO(
                post.getPostNum(),
                post.getUserNum(),
                post.getTitle(),
                post.getContent(),
                post.getImage(),
                post.getEditedAt()
                );
    }
}
