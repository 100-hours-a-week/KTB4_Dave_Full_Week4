package dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class Comment {
    @NotNull
    private long commentNum;
    @NotNull
    private long userNum;
    @NotNull
    private int parentType; // ENUM으로 관리할 생각해야 함
    @NotNull
    private long parentNum;
    @NotBlank
    private String content;
    private boolean isEdited = false;
    private boolean isDeleted = false;
    @NotBlank
    private LocalDateTime saveTime;

}
