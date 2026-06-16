package com.example.community.domain.post;

import java.time.LocalDateTime;

public record PostEditRecordDTO(
        long postNum,
        long userNum,
        String title,
        String content,
        String image,
        LocalDateTime writeTime
) {
    public static PostEditRecordDTO from(PostDTO post){
        return new PostEditRecordDTO(
                post.getPostNum(),
                post.getUserNum(),
                post.getTitle(),
                post.getContent(),
                post.getImage(),
                post.getSaveTime()
                );
    }
}
