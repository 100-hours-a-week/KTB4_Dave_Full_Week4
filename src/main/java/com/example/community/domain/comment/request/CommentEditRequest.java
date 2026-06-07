package com.example.community.domain.comment.request;

import jakarta.validation.constraints.NotNull;

public record CommentEditRequest(
        @NotNull
        Long commentNum,
        String content
) {
}
