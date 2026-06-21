package com.example.community.domain.comment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentToCommentRequest(
        @NotBlank
        String content,
        @NotNull
        Long parentNum
) {
}
