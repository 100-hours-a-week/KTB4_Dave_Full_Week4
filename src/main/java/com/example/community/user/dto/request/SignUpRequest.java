package com.example.community.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record SignUpRequest (
        @NotBlank
        @Email
        String email,
        @NotBlank
        @Size(min=8, max=20)
        @Pattern(regexp = UserValidationPatterns.PASSWORD)
        String password,
        @NotBlank
        String passwordConfirm,
        @NotBlank
        @Size(max=10)
        String nickname,
        MultipartFile imageFile
){}
