package dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserResponse(
        @NotNull
        long user_id,
        @NotBlank
        @Email
        String email,
        @NotBlank
        String nickname,
        String profile_image
) {
}
