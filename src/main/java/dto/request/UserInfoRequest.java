package dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserInfoRequest(
        @NotBlank
        String nickname,
        String profileImage
) {}
