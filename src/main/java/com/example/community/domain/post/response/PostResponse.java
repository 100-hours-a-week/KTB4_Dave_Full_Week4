package com.example.community.domain.post.response;
import com.example.community.domain.post.Post;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.LocalDateTime;

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
        LocalDateTime writeTime
) {
    public static PostResponse from(Post post, UserInfoResponse userInfoResponse){
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
                post.isEdited(),
                post.getWriteTime()
        );
    }
}
