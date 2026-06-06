package com.example.community.domain.comment.response;

import java.time.LocalDateTime;

public record CommentResponse(
        long commentNum,
        long postNum,
        long parentNum,
        int depth,
        String nickname,
        boolean isEdited,
        boolean isDeleted,
        LocalDateTime saveTime
        ) {
}
