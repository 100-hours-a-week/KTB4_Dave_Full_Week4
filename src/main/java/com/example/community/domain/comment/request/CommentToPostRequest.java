package com.example.community.domain.comment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentToPostRequest(
        @NotNull
        Long postNum,
        @NotBlank
        String content
) {

}
