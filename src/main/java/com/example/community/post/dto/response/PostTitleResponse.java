package com.example.community.post.dto.response;

import com.example.community.post.entity.Post;
import com.example.community.user.entity.UserLikePost;
import com.example.community.util.ImageUrlBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostTitleResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean blind,
        OffsetDateTime writeAt
) {
    public static PostTitleResponse from(
            UserLikePost userLikePost,
            ImageUrlBuilder imageUrlBuilder
    ) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        Post post = userLikePost.getPost();
        return new PostTitleResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                imageUrlBuilder.build(post.getUserInfo().getProfileImage()),
                post.getMaskedTitle(),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getCommentCount(),
                post.isBlind(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }

    public static PostTitleResponse from(
            Post post,
            ImageUrlBuilder imageUrlBuilder
    ) {
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostTitleResponse(
                post.getPostNum(),
                post.getUserInfo().getNickname(),
                imageUrlBuilder.build(post.getUserInfo().getProfileImage()),
                post.getMaskedTitle(),
                post.getPostState().getViewCount(),
                post.getPostState().getLikeCount(),
                post.getPostState().getCommentCount(),
                post.isBlind(),
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
