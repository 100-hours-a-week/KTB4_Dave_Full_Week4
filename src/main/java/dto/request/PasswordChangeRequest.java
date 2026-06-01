package dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank
        String password,
        @NotBlank
        String nextPassword,
        @NotBlank
        String passwordConfirm
) {
}
