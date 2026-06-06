package com.example.community.domain.comment.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        @NotNull
        Long postNum,
        @NotBlank
        String content,
        Long parentNum,
        @Min(0)
        @Max(3)
        int depth
) {
}
