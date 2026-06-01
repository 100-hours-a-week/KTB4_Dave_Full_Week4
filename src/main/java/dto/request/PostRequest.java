package dto.request;

import jakarta.validation.constraints.NotBlank;

public record PostRequest(
        @NotBlank
        String title,
        @NotBlank
        String content,
        String image
) {
}
