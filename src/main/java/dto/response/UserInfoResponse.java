package dto.response;

import jakarta.validation.constraints.NotBlank;

public record UserInfoResponse(
        @NotBlank
        String nickname,
        String profile_image
) {
}
