package domain.post.response;
import java.time.LocalDateTime;

public record PostResponse(
        long postNum,
        String nickname,
        String title,
        String content,
        String image,
        int view,
        int like,
        int numberOfComment,
        int numberOfReport,
        boolean isEdited,
        LocalDateTime saveTime
) {
}
