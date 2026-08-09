package com.example.community.post.dto.response;

import com.example.community.util.ImageUrlBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PopularPostTitleResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        OffsetDateTime writeAt
) {
    private static final String DELETED_USER_NICKNAME = "알 수 없음";

    public PopularPostTitleResponse(
            long postNum,
            String nickname,
            String profileImage,
            Instant authorDeletedAt,
            String title,
            Instant writeAt
    ) {
        this(
                postNum,
                authorDeletedAt == null ? nickname : DELETED_USER_NICKNAME,
                authorDeletedAt == null ? profileImage : null,
                title,
                writeAt.atOffset(ZoneOffset.of("+09:00"))
        );
    }

    public PopularPostTitleResponse withProfileImageUrl(
            ImageUrlBuilder imageUrlBuilder
    ) {
        return new PopularPostTitleResponse(
                postNum,
                nickname,
                imageUrlBuilder.build(profileImage),
                title,
                writeAt
        );
    }
}
