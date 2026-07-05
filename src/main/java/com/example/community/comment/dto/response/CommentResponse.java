package com.example.community.comment.dto.response;

import com.example.community.comment.dto.CommentDTO;
import com.example.community.user.dto.UserInfoDTO;
import com.example.community.user.dto.response.UserInfoResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record CommentResponse(
        long commentNum,
        long postNum,
        Long parentNum,
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
                        userInfoDTO.getNickname(),
                        userInfoDTO.getProfileImage(),
                        comment.getContent(),
                        comment.getEditedAt() != null,
                        comment.isDeleted(),
                        comment.getWriteAt().atOffset(kstOffset)
                );
        }
}
