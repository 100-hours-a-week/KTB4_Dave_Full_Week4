package com.example.community.domain.comment.response;

public record CommentAddResponse(
        int numberOfComments,
        CommentResponse comment
) {
}
