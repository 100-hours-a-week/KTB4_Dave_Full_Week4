package dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CommentResponse(
        long commentNum,
        long postNum,
        long parentNum,
        int depth,
        String nickname,
        boolean isEdited,
        boolean isDeleted,
        LocalDateTime saveTime
        ) {
}
