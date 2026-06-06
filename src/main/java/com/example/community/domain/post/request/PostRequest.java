package com.example.community.domain.post.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank
        @Size(max=26)
        String title,
        @NotBlank
        String content,
        String image
) {
}
