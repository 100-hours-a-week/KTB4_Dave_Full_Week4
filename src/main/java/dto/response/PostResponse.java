package dto.response;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record PostResponse(
        long postNum,
        @NotBlank
        String nickname,
        @NotBlank
        String title,
        @NotBlank
        String content,
        String image,
        int view,
        int like,
        int number_of_comments,
        int number_of_reports,
        boolean isEdited,
        LocalDateTime saveTime
) {
}
