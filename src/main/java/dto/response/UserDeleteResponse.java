package dto.response;

import jakarta.validation.constraints.NotNull;

public record UserDeleteResponse(
        long user_id,
        boolean isDeleted
) {
}
