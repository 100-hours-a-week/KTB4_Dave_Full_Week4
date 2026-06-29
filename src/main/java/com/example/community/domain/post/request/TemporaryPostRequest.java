package com.example.community.domain.post.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record TemporaryPostRequest(
        Long postNum,
        @NotBlank
        @Size(max=26)
        String title,
        @NotBlank
        String content,
        MultipartFile image
) {
}
