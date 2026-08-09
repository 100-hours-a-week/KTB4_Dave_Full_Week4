package com.example.community.post.dto.query;

import com.example.community.post.entity.PostState;

public record PostStateData(
        int viewCount,
        int likeCount,
        int reportCount,
        int commentCount
) {
    public boolean isBlind() {
        return reportCount >= PostState.BLIND_REPORT_THRESHOLD;
    }
}
