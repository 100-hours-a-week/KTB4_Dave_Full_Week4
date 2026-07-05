package com.example.community.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank
        @Size(min=8, max=20)
        String password,
        @NotBlank
        @Size(min=8, max=20)
        String nextPassword,
        @NotBlank
        @Size(min=8, max=20)
        String passwordConfirm
) {
}
