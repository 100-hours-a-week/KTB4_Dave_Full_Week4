package com.example.community.domain.post.response;
import com.example.community.domain.post.PostDTO;
import com.example.community.domain.user.UserInfoDTO;
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

    public static PostResponse from(PostDTO post, UserInfoDTO userInfoDTO){
        ZoneOffset kstOffset = ZoneOffset.of("+09:00");
        return new PostResponse(
                post.getPostNum(),
                userInfoDTO.nickname(),
                userInfoDTO.profileImage(),
                post.getTitle(),
                post.getContent(),
                post.getImage(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getReportCount(),
                post.getCommentCount(),
                post.getEditedAt() != null,
                post.getWriteAt().atOffset(kstOffset)
        );
    }
}
