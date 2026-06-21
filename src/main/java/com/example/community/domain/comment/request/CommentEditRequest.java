package com.example.community.domain.comment.request;


import jakarta.validation.constraints.NotBlank;

public record CommentEditRequest(
        @NotBlank
        String content
) {
}
