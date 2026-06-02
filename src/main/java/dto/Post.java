package dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class Post {
    @NotNull
    private long postNum;
    @NotNull
    private long userNum;
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private String image;
    @Positive
    private long views = 0;
    @Positive
    private long likes = 0;
    @Positive
    private long comments = 0;
    @Min(0) @Max(5)
    private long reports = 0;
    @NotNull
    private boolean isEdited = false;
    @NotBlank
    private LocalDateTime saveTime;
}
