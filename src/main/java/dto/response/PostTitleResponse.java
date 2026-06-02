package dto.response;

import java.time.LocalDateTime;

public record PostTitleResponse(
        long postNum,
        String nickname,
        String title,
        int view,
        int like,
        int number_of_comment,
        int number_of_report,
        LocalDateTime saveTime
) {
}
