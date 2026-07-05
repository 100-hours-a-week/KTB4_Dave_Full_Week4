package com.example.community.comment.dto.response;

public record CommentAddResponse(
        int numberOfComments,
        CommentResponse comment
) {
}
