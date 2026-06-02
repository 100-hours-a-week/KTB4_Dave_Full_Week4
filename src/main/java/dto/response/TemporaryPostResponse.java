package dto.response;

import jakarta.validation.constraints.NotBlank;

public record TemporaryPostResponse(
        @NotBlank
        String title,
        @NotBlank
        String content,
        String image
) {
}
