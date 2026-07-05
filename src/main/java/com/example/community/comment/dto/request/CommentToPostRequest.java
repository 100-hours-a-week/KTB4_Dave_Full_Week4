package com.example.community.comment.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentToPostRequest(
        @NotBlank
        String content
) {

}
