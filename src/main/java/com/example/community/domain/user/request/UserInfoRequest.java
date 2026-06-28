package com.example.community.domain.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record UserInfoRequest(
        @NotNull
        Long profileId,
        @NotBlank
        @Size(max=10)
        String nickname,
        MultipartFile imageFile
) {}
