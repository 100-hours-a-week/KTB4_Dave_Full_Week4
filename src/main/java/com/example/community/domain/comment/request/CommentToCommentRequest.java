package com.example.community.domain.comment.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CommentToCommentRequest(
        @NotBlank
        String content,
        Long parentNum,
        @Min(0)
        @Max(3)
        int depth
) {
}
