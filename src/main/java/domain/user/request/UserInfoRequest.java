package domain.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserInfoRequest(
        @NotBlank
        @Size(max=10)
        String nickname,
        String profileImage
) {}
