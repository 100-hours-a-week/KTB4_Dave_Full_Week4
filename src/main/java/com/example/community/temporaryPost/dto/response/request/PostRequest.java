package com.example.community.temporaryPost.dto.response.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record PostRequest(
        @NotBlank
        @Size(max=26)
        String title,
        @NotBlank
        String content,
        MultipartFile image
) {
}
