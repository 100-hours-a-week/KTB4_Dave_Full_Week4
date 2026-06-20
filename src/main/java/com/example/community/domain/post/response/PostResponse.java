package com.example.community.domain.post.response;
import com.example.community.domain.post.PostDTO;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record PostResponse(
        long postNum,
        String nickname,
        String profileImage,
        String title,
        String content,
        String image,
        int view,
        int like,
        int report,
        int numberOfComments,
        boolean isEdited,
        OffsetDateTime writeAt
) {
    public static PostResponse from(PostDTO post, UserInfoResponse userInfoResponse){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostResponse(
                post.getPostNum(),
                userInfoResponse.nickname(),
                userInfoResponse.profileImage(),
                post.getTitle(),
                post.getContent(),
                post.getImage(),
                post.getView(),
                post.getLike(),
                post.getReport(),
                post.getNumberOfComments(),
                post.getEditedAt() != null,
                post.getWriteTime().atOffset(kstOffset)
        );
    }
}
