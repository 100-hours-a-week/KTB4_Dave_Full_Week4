package com.example.community.domain.post.response;

import java.time.LocalDateTime;

public record PostTitleResponse(
        long postNum,
        String nickname,
        String title,
        int view,
        int like,
        int numberOfComment,
        int numberOfReport,
        LocalDateTime saveTime
) {
}
