package com.example.community.domain.post;

import java.time.LocalDateTime;

public record PostEditRecord(
        long postNum,
        long userNum,
        String title,
        String content,
        String image,
        LocalDateTime writeTime
) {
    public static PostEditRecord from(Post post){
        return new PostEditRecord(
                post.getPostNum(),
                post.getUserNum(),
                post.getTitle(),
                post.getContent(),
                post.getImage(),
                post.getSaveTime()
                );
    }
}
