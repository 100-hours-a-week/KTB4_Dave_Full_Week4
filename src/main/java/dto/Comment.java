package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

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
