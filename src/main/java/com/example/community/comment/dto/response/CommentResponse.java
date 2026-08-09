package com.example.community.comment.dto.response;
import com.example.community.comment.entity.Comment;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record   CommentResponse(
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

        private static final String DELETED_USER_NICKNAME = "탈퇴한 사용자";
        private static final String DELETED_COMMENT_CONTENT = "삭제된 댓글입니다.";

        public CommentResponse(
                long commentNum,
                long postNum,
                Long parentNum,
                int depth,
                String nickname,
                String profileImage,
                Instant authorDeletedAt,
                String content,
                long childCount,
                Instant editedAt,
                Instant deletedAt,
                Instant writeAt
        ) {
                this(
                        commentNum,
                        postNum,
                        parentNum,
                        depth,
                        displayNickname(nickname, authorDeletedAt),
                        displayProfileImage(profileImage, authorDeletedAt),
                        displayContent(content, deletedAt),
                        childCount,
                        editedAt != null,
                        deletedAt != null,
                        writeAt.atOffset(ZoneOffset.of("+09:00"))
                );
        }

        public static CommentResponse from(Comment comment){
                return new CommentResponse(
                        comment.getCommentNum(),
                        comment.getPost().getPostNum(),
                        comment.getComment() != null ? comment.getComment().getCommentNum() : null,
                        comment.getDepth(),
                        comment.getUserInfo().getNickname(),
                        comment.getUserInfo().getProfileImage(),
                        comment.getUserInfo().getDeletedAt(),
                        comment.getContent(),
                        comment.getChildCount(),
                        comment.getEditedAt(),
                        comment.getDeletedAt(),
                        comment.getWriteAt()
                );
        }


        public static CommentResponse adminFrom(Comment comment){
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

        private static String displayNickname(String nickname, Instant deletedAt) {
                return deletedAt == null ? nickname : DELETED_USER_NICKNAME;
        }

        private static String displayProfileImage(String image, Instant deletedAt) {
                return deletedAt == null ? image : null;
        }

        private static String displayContent(String content, Instant deletedAt) {
                return deletedAt == null ? content : DELETED_COMMENT_CONTENT;
        }
}
