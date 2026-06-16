package com.example.community.domain.comment.response;

import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.LocalDateTime;

public record CommentResponse(
        long commentNum,
        long postNum,
        long parentNum,
        int depth,
        String nickname,
        String profileImage,
        String content,
        boolean edited,
        boolean deleted,
        LocalDateTime writeTime
        ) {
        public static CommentResponse from(CommentDTO comment, UserInfoResponse userInfoResponse){
                return new CommentResponse(
                        comment.getCommentNum(),
                        comment.getPostNum(),
                        comment.getParentNum(),
                        comment.getDepth(),
                        userInfoResponse.nickname(),
                        userInfoResponse.profileImage(),
                        comment.getContent(),
                        comment.isEdited(),
                        comment.isDeleted(),
                        comment.getWriteTime()
                );
        }
}
