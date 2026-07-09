package com.example.community.comment.dto.response;
import com.example.community.comment.entity.Comment;

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
        long childCount,
        boolean edited,
        boolean deleted,
        OffsetDateTime writeAt
        ) {

        public static CommentResponse from(Comment comment){
                ZoneOffset kstOffset = ZoneOffset.of("+09:00");
                return new CommentResponse(
                        comment.getCommentNum(),
                        comment.getPost().getPostNum(),
                        comment.getComment() != null ? comment.getComment().getCommentNum() : null,
                        comment.getDepth(),
                        comment.getUserInfo().getNickname(),
                        comment.getUserInfo().getProfileImage(),
                        comment.getContent(),
                        comment.getChildCount(),
                        comment.getEditedAt() != null,
                        comment.isDeleted(),
                        comment.getWriteAt().atOffset(kstOffset)
                );
        }
}
