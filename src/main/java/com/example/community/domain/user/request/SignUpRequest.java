package com.example.community.domain.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignUpRequest (
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min=8, max=20)
        String password,
        @NotBlank
        String passwordConfirm,
        @NotBlank
        @Size(max=10)
        String nickname,
        String profileImage
){}