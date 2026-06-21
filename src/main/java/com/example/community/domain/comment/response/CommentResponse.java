package com.example.community.domain.comment.response;

import com.example.community.domain.comment.CommentDTO;
import com.example.community.domain.user.UserInfoDTO;
import com.example.community.domain.user.response.UserInfoResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
        OffsetDateTime writeAt
        ) {
        public static CommentResponse of(CommentDTO comment, UserInfoResponse userInfoResponse){
                ZoneOffset kstOffset = ZoneOffset.of("+09:00");
                return new CommentResponse(
                        comment.getCommentNum(),
                        comment.getPostNum(),
                        comment.getParentNum(),
                        comment.getDepth(),
                        userInfoResponse.nickname(),
                        userInfoResponse.profileImage(),
                        comment.getContent(),
                        comment.getEditedAt() != null,
                        comment.isDeleted(),
                        comment.getWriteAt().atOffset(kstOffset)
                );
        }
        public static CommentResponse of(CommentDTO comment, UserInfoDTO userInfoDTO){
                ZoneOffset kstOffset = ZoneOffset.of("+09:00");
                return new CommentResponse(
                        comment.getCommentNum(),
                        comment.getPostNum(),
                        comment.getParentNum(),
                        comment.getDepth(),
                        userInfoDTO.nickname(),
                        userInfoDTO.profileImage(),
                        comment.getContent(),
                        comment.getEditedAt() != null,
                        comment.isDeleted(),
                        comment.getWriteAt().atOffset(kstOffset)
                );
        }
}
