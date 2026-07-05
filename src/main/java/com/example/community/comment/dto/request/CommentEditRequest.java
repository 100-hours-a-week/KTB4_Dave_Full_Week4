package com.example.community.comment.dto.request;


import jakarta.validation.constraints.NotBlank;

public record CommentEditRequest(
        @NotBlank
        String content
) {
}
