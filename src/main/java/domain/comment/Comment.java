package domain.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@NoArgsConstructor
public class Comment {
    private long commentNum;
    private long postNum;
    private long parentNum;
    private int depth;
    private long userNum;
    @NotBlank
    private String content;
    private boolean isEdited = false;
    private boolean isDeleted = false;
    @NotNull
    private LocalDateTime saveTime;

}
