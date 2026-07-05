package com.example.community.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentToCommentRequest(
        @NotBlank
        String content,
        @NotNull
        Long parentNum
) {
}
