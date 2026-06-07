package com.example.community.domain.post.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostEditRequest(
        @NotNull
        Long postNum,
        @NotBlank
        @Size(max=26)
        String title,
        @NotBlank
        String content,
        String image
) {

}
