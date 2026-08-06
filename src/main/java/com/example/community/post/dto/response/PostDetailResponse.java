package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import com.example.community.util.ImageUrlBuilder;

import java.time.OffsetDateTime;

public record PostDetailResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        String content,
        String image,
        String objectKey,
        int viewCount,
        int likeCount,
        int reportCount,
        int commentCount,
        boolean isEdited,
        OffsetDateTime writeAt
) {
    public static PostDetailResponse from(
            Post post,
            ImageUrlBuilder imageUrlBuilder
    ) {
        PostResponse response = PostResponse.from(post, imageUrlBuilder);
        return new PostDetailResponse(
                response.postNum(),
                response.nickname(),
                response.profileImage(),
                response.title(),
                response.content(),
                response.image(),
                post.getImage(),
                response.viewCount(),
                response.likeCount(),
                response.reportCount(),
                response.commentCount(),
                response.isEdited(),
                response.writeAt()
        );
    }
}
