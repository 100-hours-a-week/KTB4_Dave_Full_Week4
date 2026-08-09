package com.example.community.post.dto.query;

import java.time.Instant;

public record PostBodyData(
        long postNum,
        String nickname,
        String profileImage,
        Instant authorDeletedAt,
        String title,
        String content,
        String imageObjectKey,
        Instant editedAt,
        Instant writeAt
) {
    private static final String DELETED_USER_NICKNAME = "알 수 없음";

    public String displayNickname() {
        return authorDeletedAt == null ? nickname : DELETED_USER_NICKNAME;
    }

    public String displayProfileImage() {
        return authorDeletedAt == null ? profileImage : null;
    }
}
